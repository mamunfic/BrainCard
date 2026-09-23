package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    indices = [Index(value = ["folderId"]), Index(value = ["dueDate"])]
)
data class FlashcardEntity(
    @PrimaryKey
    val id: String,
    val folderId: String = "general",
    val question: String,
    val answer: String,
    val tags: String = "", // Comma-separated tags
    val reviewStep: Int = 0, // 0..4 (indices for intervals [1, 3, 7, 14, 30])
    val dueDate: String, // Format: "yyyy-MM-dd"
    val lastReviewed: String? = null,
    val imageUri: String? = null, // Optional local image URI for rich visual cards
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
