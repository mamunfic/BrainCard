package com.example.util

import com.example.data.model.FlashcardEntity
import com.example.data.model.FolderEntity
import com.example.data.repository.BrainCardRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    /**
     * Generates a simple, clean, and compliant CSV string representing study progress,
     * card totals, and the due status of cards.
     */
    fun generateProgressCsv(
        folders: List<FolderEntity>,
        cards: List<FlashcardEntity>,
        activeFolderId: String?,
        exportAllFolders: Boolean
    ): String {
        val todayStr = BrainCardRepository.getTodayDateString()
        val tomorrowStr = BrainCardRepository.getDateStringAfterDays(1)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val folderMap = folders.associateBy { it.id }
        val targetFolder = if (!exportAllFolders && activeFolderId != null) {
            folderMap[activeFolderId]
        } else null

        val filteredCards = if (exportAllFolders || targetFolder == null) {
            cards
        } else {
            cards.filter { it.folderId == targetFolder.id }
        }

        val totalCards = filteredCards.size
        val dueNowCards = filteredCards.count { it.dueDate <= todayStr }
        val dueTomorrowCards = filteredCards.count { it.dueDate == tomorrowStr }
        val laterScheduledCards = filteredCards.count { it.dueDate > tomorrowStr }

        val sb = StringBuilder()

        // 1. Summary Meta Section
        sb.append("Section,Metric,Value\n")
        sb.append("Summary,Report Type,BrainCard Study Progress\n")
        sb.append("Summary,Generated At,${escapeCsv(timestamp)}\n")
        sb.append("Summary,Folder Scope,${escapeCsv(if (exportAllFolders || targetFolder == null) "All Folders" else targetFolder.name)}\n")
        sb.append("Summary,Total Cards,$totalCards\n")
        sb.append("Summary,Due Now Cards,$dueNowCards\n")
        sb.append("Summary,Tomorrow Due Cards,$dueTomorrowCards\n")
        sb.append("Summary,Later Scheduled Cards,$laterScheduledCards\n")

        // Per-folder breakdown if exporting all
        if (exportAllFolders && folders.isNotEmpty()) {
            sb.append("\nFolder Summary,Folder Name,Total,Due Now,Tomorrow,Later\n")
            for (f in folders) {
                val fCards = cards.filter { it.folderId == f.id }
                val fTotal = fCards.size
                val fDueNow = fCards.count { it.dueDate <= todayStr }
                val fTomorrow = fCards.count { it.dueDate == tomorrowStr }
                val fLater = fCards.count { it.dueDate > tomorrowStr }
                sb.append("Folder Summary,${escapeCsv(f.name)},$fTotal,$fDueNow,$fTomorrow,$fLater\n")
            }
        }

        // Blank line before card details
        sb.append("\n")

        // 2. Card Level Breakdown with Due Status
        sb.append("Card ID,Folder,Due Status,Due Date,Review Step,Interval,Question,Answer,Tags,Last Reviewed\n")
        for (card in filteredCards) {
            val folderName = folderMap[card.folderId]?.name ?: card.folderId
            val dueStatus = when {
                card.dueDate < todayStr -> "OVERDUE (Due $card.dueDate)"
                card.dueDate == todayStr -> "DUE NOW"
                card.dueDate == tomorrowStr -> "DUE TOMORROW"
                else -> "SCHEDULED (Due ${card.dueDate})"
            }

            val intervalDays = if (card.reviewStep in BrainCardRepository.REVIEW_INTERVALS.indices) {
                "${BrainCardRepository.REVIEW_INTERVALS[card.reviewStep]} days"
            } else {
                "30 days"
            }

            val lastRevFormatted = card.lastReviewed?.let {
                BrainCardRepository.formatReadableDate(it)
            } ?: "Never"

            sb.append(escapeCsv(card.id)).append(",")
            sb.append(escapeCsv(folderName)).append(",")
            sb.append(escapeCsv(dueStatus)).append(",")
            sb.append(escapeCsv(card.dueDate)).append(",")
            sb.append(card.reviewStep).append(",")
            sb.append(escapeCsv(intervalDays)).append(",")
            sb.append(escapeCsv(card.question)).append(",")
            sb.append(escapeCsv(card.answer)).append(",")
            sb.append(escapeCsv(card.tags)).append(",")
            sb.append(escapeCsv(lastRevFormatted)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Escapes standard CSV values with RFC-4180 rules.
     */
    fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
