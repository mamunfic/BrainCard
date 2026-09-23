package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.FlashcardEntity
import com.example.data.model.FolderEntity
import com.example.data.repository.BrainCardRepository
import com.example.util.CsvExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BrainCard", appName)
  }

  @Test
  fun `csv export includes card totals and due status`() {
    val folders = listOf(
      FolderEntity(id = "folder_1", name = "Test Folder", createdAt = 1000L)
    )
    val today = BrainCardRepository.getTodayDateString()
    val tomorrow = BrainCardRepository.getDateStringAfterDays(1)
    val future = BrainCardRepository.getDateStringAfterDays(7)

    val cards = listOf(
      FlashcardEntity(
        id = "card_1",
        folderId = "folder_1",
        question = "What is, a comma?",
        answer = "A \"punctuation\" mark",
        tags = "grammar, punctuation",
        reviewStep = 0,
        dueDate = today,
        createdAt = 1000L
      ),
      FlashcardEntity(
        id = "card_2",
        folderId = "folder_1",
        question = "Question 2",
        answer = "Answer 2",
        tags = "tag2",
        reviewStep = 1,
        dueDate = tomorrow,
        createdAt = 1001L
      ),
      FlashcardEntity(
        id = "card_3",
        folderId = "folder_1",
        question = "Question 3",
        answer = "Answer 3",
        tags = "tag3",
        reviewStep = 2,
        dueDate = future,
        createdAt = 1002L
      )
    )

    val csv = CsvExportHelper.generateProgressCsv(
      folders = folders,
      cards = cards,
      activeFolderId = "folder_1",
      exportAllFolders = false
    )

    // Verify card totals in summary
    assertTrue(csv.contains("Summary,Total Cards,3"))
    assertTrue(csv.contains("Summary,Due Now Cards,1"))
    assertTrue(csv.contains("Summary,Tomorrow Due Cards,1"))
    assertTrue(csv.contains("Summary,Later Scheduled Cards,1"))

    // Verify due status of individual cards
    assertTrue(csv.contains("DUE NOW"))
    assertTrue(csv.contains("DUE TOMORROW"))
    assertTrue(csv.contains("SCHEDULED"))

    // Verify proper CSV escaping
    assertTrue(csv.contains("\"What is, a comma?\""))
    assertTrue(csv.contains("\"A \"\"punctuation\"\" mark\""))
  }
}

