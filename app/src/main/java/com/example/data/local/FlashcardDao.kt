package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getCardsByFolder(folderId: String): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteCard(id: String)

    @Query("DELETE FROM flashcards WHERE folderId = :folderId")
    suspend fun deleteCardsByFolder(folderId: String)

    @Query("SELECT COUNT(*) FROM flashcards")
    suspend fun getCardCount(): Int
}
