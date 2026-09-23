package com.example.data.repository

import com.example.data.local.FlashcardDao
import com.example.data.local.FolderDao
import com.example.data.model.FlashcardEntity
import com.example.data.model.FolderEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class BrainCardRepository(
    private val folderDao: FolderDao,
    private val flashcardDao: FlashcardDao
) {
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val allCards: Flow<List<FlashcardEntity>> = flashcardDao.getAllCards()

    fun getCardsByFolder(folderId: String): Flow<List<FlashcardEntity>> {
        return flashcardDao.getCardsByFolder(folderId)
    }

    suspend fun insertFolder(name: String): FolderEntity {
        val folder = FolderEntity(
            id = "folder_${UUID.randomUUID().toString().take(8)}",
            name = name,
            createdAt = System.currentTimeMillis()
        )
        folderDao.insertFolder(folder)
        return folder
    }

    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folderId: String) {
        if (folderId == "general") return // Cannot delete General folder
        folderDao.deleteFolder(folderId)
        flashcardDao.deleteCardsByFolder(folderId)
    }

    suspend fun insertCard(
        folderId: String,
        question: String,
        answer: String,
        tags: String,
        imageUri: String? = null
    ): FlashcardEntity {
        val todayStr = getTodayDateString()
        val card = FlashcardEntity(
            id = "card_${UUID.randomUUID().toString().take(8)}",
            folderId = folderId,
            question = question,
            answer = answer,
            tags = tags,
            reviewStep = 0,
            dueDate = todayStr,
            lastReviewed = null,
            imageUri = imageUri,
            createdAt = System.currentTimeMillis()
        )
        flashcardDao.insertCard(card)
        return card
    }

    suspend fun insertCards(cards: List<FlashcardEntity>) {
        for (card in cards) {
            flashcardDao.insertCard(card)
        }
    }

    suspend fun updateCard(card: FlashcardEntity) {
        flashcardDao.updateCard(card)
    }

    suspend fun deleteCard(cardId: String) {
        flashcardDao.deleteCard(cardId)
    }

    suspend fun populateInitialDataIfEmpty() {
        val folderCount = folderDao.getFolderCount()
        if (folderCount == 0) {
            val generalFolder = FolderEntity(
                id = "general",
                name = "General",
                createdAt = System.currentTimeMillis()
            )
            val scienceFolder = FolderEntity(
                id = "science",
                name = "Science & Tech",
                createdAt = System.currentTimeMillis() + 10
            )
            val languagesFolder = FolderEntity(
                id = "languages",
                name = "Languages",
                createdAt = System.currentTimeMillis() + 20
            )
            folderDao.insertFolder(generalFolder)
            folderDao.insertFolder(scienceFolder)
            folderDao.insertFolder(languagesFolder)

            val today = getTodayDateString()
            val tomorrow = getDateStringAfterDays(1)
            val dayAfter3 = getDateStringAfterDays(3)

            val seedCards = listOf(
                FlashcardEntity(
                    id = "card_seed_1",
                    folderId = "general",
                    question = "What is the Spaced Repetition effect?",
                    answer = "A learning technique where reviews are spaced at increasing intervals (e.g. 1d, 3d, 7d, 14d, 30d) to optimize long-term retention and fight the forgetting curve.",
                    tags = "learning, psychology, braincard",
                    reviewStep = 0,
                    dueDate = today,
                    createdAt = System.currentTimeMillis()
                ),
                FlashcardEntity(
                    id = "card_seed_2",
                    folderId = "general",
                    question = "How does BrainCard interval scheduling work?",
                    answer = "When you tap 'Remembered', the interval expands: 1 → 3 → 7 → 14 → 30 days.\nIf you tap 'Forgot', the card resets to 1 day for prompt reinforcement.",
                    tags = "spaced-repetition, study",
                    reviewStep = 1,
                    dueDate = today,
                    createdAt = System.currentTimeMillis() + 1
                ),
                FlashcardEntity(
                    id = "card_seed_3",
                    folderId = "science",
                    question = "What is the primary function of Mitochondria?",
                    answer = "Mitochondria generate most of the cell's supply of adenosine triphosphate (ATP), used as a source of chemical energy (the 'powerhouse of the cell').",
                    tags = "biology, cells, science",
                    reviewStep = 0,
                    dueDate = today,
                    createdAt = System.currentTimeMillis() + 2
                ),
                FlashcardEntity(
                    id = "card_seed_4",
                    folderId = "science",
                    question = "What does Jetpack Compose use to trigger UI updates?",
                    answer = "Jetpack Compose observes reactive State (like StateFlow or mutableStateOf). When state changes, Compose recomposes only the functions that read that state.",
                    tags = "android, kotlin, compose",
                    reviewStep = 2,
                    dueDate = tomorrow,
                    createdAt = System.currentTimeMillis() + 3
                ),
                FlashcardEntity(
                    id = "card_seed_5",
                    folderId = "languages",
                    question = "How do you say 'Thank you very much' in Japanese?",
                    answer = "どうもありがとうございます (Dōmo arigatō gozaimasu)",
                    tags = "japanese, phrase, vocabulary",
                    reviewStep = 1,
                    dueDate = dayAfter3,
                    createdAt = System.currentTimeMillis() + 4
                )
            )

            for (card in seedCards) {
                flashcardDao.insertCard(card)
            }
        }
    }

    companion object {
        val REVIEW_INTERVALS = intArrayOf(1, 3, 7, 14, 30)

        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun getDateStringAfterDays(days: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, days)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(cal.time)
        }

        fun formatReadableDate(dateString: String): String {
            return try {
                val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = inputSdf.parse(dateString) ?: return dateString
                val outputSdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
                outputSdf.format(date)
            } catch (e: Exception) {
                dateString
            }
        }
    }
}
