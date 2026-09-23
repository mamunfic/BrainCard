package com.example.util

import com.example.data.model.FlashcardEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

data class ParsedCard(
    val question: String,
    val answer: String,
    val tags: String
)

object CsvImportHelper {

    /**
     * Parses CSV or TSV (Anki, Quizlet, Spreadsheets) content into cards.
     * Handles quoted fields, comma, tab, or semicolon delimiters.
     */
    fun parseDelimitedText(text: String): List<ParsedCard> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val results = mutableListOf<ParsedCard>()
        val firstLine = lines.first()

        // Detect delimiter: tab, semicolon, or comma
        val delimiter = when {
            firstLine.count { it == '\t' } >= 1 -> '\t'
            firstLine.count { it == ';' } >= 1 -> ';'
            else -> ','
        }

        // Check if first row is header
        var startIndex = 0
        val lowerFirst = firstLine.lowercase()
        if (lowerFirst.contains("question") || lowerFirst.contains("front") || lowerFirst.contains("term")) {
            startIndex = 1
        }

        for (i in startIndex until lines.size) {
            val row = parseCsvRow(lines[i], delimiter)
            if (row.size >= 2) {
                val q = row[0].trim()
                val a = row[1].trim()
                val t = if (row.size >= 3) row[2].trim() else ""
                if (q.isNotEmpty() && a.isNotEmpty()) {
                    results.add(ParsedCard(q, a, t))
                }
            }
        }
        return results
    }

    fun parseStream(inputStream: InputStream): List<ParsedCard> {
        val content = inputStream.bufferedReader().use(BufferedReader::readText)
        return parseDelimitedText(content)
    }

    private fun parseCsvRow(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
