package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.FlashcardEntity
import com.example.data.model.FolderEntity
import com.example.data.repository.BrainCardRepository
import com.example.util.CsvExportHelper
import com.example.util.CsvImportHelper
import com.example.util.DailyReminderReceiver
import com.example.util.ParsedCard
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class BrainCardTab {
    STUDY,
    QUIZ,
    CALENDAR,
    LIBRARY,
    STATS
}

enum class ReviewResult {
    FORGOT,
    REMEMBERED
}

data class QuizQuestion(
    val cardId: String,
    val question: String,
    val correctAnswer: String,
    val options: List<String>,
    val imageUri: String? = null
)

class BrainCardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrainCardRepository
    val ttsHelper = TextToSpeechHelper(application)

    val allFolders: StateFlow<List<FolderEntity>>
    val allCards: StateFlow<List<FlashcardEntity>>

    private val _activeFolderId = MutableStateFlow("general")
    val activeFolderId: StateFlow<String> = _activeFolderId.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _isAnswerRevealed = MutableStateFlow(false)
    val isAnswerRevealed: StateFlow<Boolean> = _isAnswerRevealed.asStateFlow()

    private val _isDarkThemeOverride = MutableStateFlow<Boolean?>(null)
    val isDarkThemeOverride: StateFlow<Boolean?> = _isDarkThemeOverride.asStateFlow()

    private val _selectedCalendarDate = MutableStateFlow(BrainCardRepository.getTodayDateString())
    val selectedCalendarDate: StateFlow<String> = _selectedCalendarDate.asStateFlow()

    private val _calendarMonthDate = MutableStateFlow(Calendar.getInstance())
    val calendarMonthDate: StateFlow<Calendar> = _calendarMonthDate.asStateFlow()

    private val _activeTab = MutableStateFlow(BrainCardTab.STUDY)
    val activeTab: StateFlow<BrainCardTab> = _activeTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Daily Reminder State
    private val _isReminderEnabled = MutableStateFlow(DailyReminderReceiver.isReminderEnabled(application))
    val isReminderEnabled: StateFlow<Boolean> = _isReminderEnabled.asStateFlow()

    private val _reminderTime = MutableStateFlow(DailyReminderReceiver.getReminderTime(application))
    val reminderTime: StateFlow<Pair<Int, Int>> = _reminderTime.asStateFlow()

    // Speed Review / Timer Mode State
    private val _isSpeedReviewActive = MutableStateFlow(false)
    val isSpeedReviewActive: StateFlow<Boolean> = _isSpeedReviewActive.asStateFlow()

    private val _speedTimerSeconds = MutableStateFlow(10)
    val speedTimerSeconds: StateFlow<Int> = _speedTimerSeconds.asStateFlow()

    // Quiz Mode State
    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex.asStateFlow()

    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _selectedQuizAnswer = MutableStateFlow<String?>(null)
    val selectedQuizAnswer: StateFlow<String?> = _selectedQuizAnswer.asStateFlow()

    private val _isQuizAnswerSubmitted = MutableStateFlow(false)
    val isQuizAnswerSubmitted: StateFlow<Boolean> = _isQuizAnswerSubmitted.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _isQuizCompleted = MutableStateFlow(false)
    val isQuizCompleted: StateFlow<Boolean> = _isQuizCompleted.asStateFlow()

    // Dialog states
    private val _showAddFolderDialog = MutableStateFlow(false)
    val showAddFolderDialog: StateFlow<Boolean> = _showAddFolderDialog.asStateFlow()

    private val _editingFolder = MutableStateFlow<FolderEntity?>(null)
    val editingFolder: StateFlow<FolderEntity?> = _editingFolder.asStateFlow()

    private val _showAddCardDialog = MutableStateFlow(false)
    val showAddCardDialog: StateFlow<Boolean> = _showAddCardDialog.asStateFlow()

    private val _editingCard = MutableStateFlow<FlashcardEntity?>(null)
    val editingCard: StateFlow<FlashcardEntity?> = _editingCard.asStateFlow()

    private val _cardToDelete = MutableStateFlow<FlashcardEntity?>(null)
    val cardToDelete: StateFlow<FlashcardEntity?> = _cardToDelete.asStateFlow()

    private val _folderToDelete = MutableStateFlow<FolderEntity?>(null)
    val folderToDelete: StateFlow<FolderEntity?> = _folderToDelete.asStateFlow()

    private val _showExportProgressDialog = MutableStateFlow(false)
    val showExportProgressDialog: StateFlow<Boolean> = _showExportProgressDialog.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val _showReminderDialog = MutableStateFlow(false)
    val showReminderDialog: StateFlow<Boolean> = _showReminderDialog.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = BrainCardRepository(db.folderDao(), db.flashcardDao())

        allFolders = repository.allFolders.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allCards = repository.allCards.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }
    }

    // Filter cards by active folder
    val activeFolderCards: StateFlow<List<FlashcardEntity>> = combine(
        allCards,
        activeFolderId
    ) { cards, folderId ->
        cards.filter { it.folderId == folderId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study cards: sorted so due cards come first, then ordered by due date
    val studyCards: StateFlow<List<FlashcardEntity>> = activeFolderCards.combine(
        MutableStateFlow(Unit)
    ) { cards, _ ->
        val todayStr = BrainCardRepository.getTodayDateString()
        cards.sortedWith(
            compareBy<FlashcardEntity> { card ->
                if (card.dueDate <= todayStr) 0 else 1
            }.thenBy { it.dueDate }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueCardsCount: StateFlow<Int> = activeFolderCards.combine(
        MutableStateFlow(Unit)
    ) { cards, _ ->
        val today = BrainCardRepository.getTodayDateString()
        cards.count { it.dueDate <= today }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val tomorrowCardsCount: StateFlow<Int> = activeFolderCards.combine(
        MutableStateFlow(Unit)
    ) { cards, _ ->
        val tomorrow = BrainCardRepository.getDateStringAfterDays(1)
        cards.count { it.dueDate == tomorrow }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCardsCount: StateFlow<Int> = activeFolderCards.combine(
        MutableStateFlow(Unit)
    ) { cards, _ -> cards.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Navigation and tab controls
    fun setActiveTab(tab: BrainCardTab) {
        _activeTab.value = tab
        if (tab == BrainCardTab.QUIZ) {
            initQuiz()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folderId: String) {
        _activeFolderId.value = folderId
        _currentCardIndex.value = 0
        _isAnswerRevealed.value = false
        _selectedCalendarDate.value = BrainCardRepository.getTodayDateString()
        if (_activeTab.value == BrainCardTab.QUIZ) {
            initQuiz()
        }
    }

    fun toggleTheme(currentDark: Boolean) {
        _isDarkThemeOverride.value = !currentDark
    }

    // Flashcard Actions
    fun revealAnswer() {
        _isAnswerRevealed.value = true
    }

    fun hideAnswer() {
        _isAnswerRevealed.value = false
    }

    fun toggleReveal() {
        _isAnswerRevealed.value = !_isAnswerRevealed.value
    }

    fun nextCard() {
        val size = studyCards.value.size
        if (size > 0) {
            _currentCardIndex.value = (_currentCardIndex.value + 1) % size
            _isAnswerRevealed.value = false
            ttsHelper.stop()
        }
    }

    fun previousCard() {
        val size = studyCards.value.size
        if (size > 0) {
            _currentCardIndex.value = (_currentCardIndex.value - 1 + size) % size
            _isAnswerRevealed.value = false
            ttsHelper.stop()
        }
    }

    fun studySpecificCard(cardId: String) {
        val list = studyCards.value
        val index = list.indexOfFirst { it.id == cardId }
        if (index != -1) {
            _currentCardIndex.value = index
            _isAnswerRevealed.value = false
            _activeTab.value = BrainCardTab.STUDY
            ttsHelper.stop()
        }
    }

    fun handleReview(cardId: String, result: ReviewResult) {
        val card = allCards.value.find { it.id == cardId } ?: return
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())

        val updatedCard = when (result) {
            ReviewResult.FORGOT -> {
                card.copy(
                    reviewStep = 0,
                    dueDate = BrainCardRepository.getDateStringAfterDays(1),
                    lastReviewed = nowIso
                ).also {
                    showToast("Forgot → next review in 1 day")
                }
            }
            ReviewResult.REMEMBERED -> {
                val step = card.reviewStep.coerceIn(0, BrainCardRepository.REVIEW_INTERVALS.size - 1)
                val interval = BrainCardRepository.REVIEW_INTERVALS[step]
                val nextStep = (step + 1).coerceAtMost(BrainCardRepository.REVIEW_INTERVALS.size - 1)
                val intervalStr = if (interval == 1) "1 day" else "$interval days"

                card.copy(
                    reviewStep = nextStep,
                    dueDate = BrainCardRepository.getDateStringAfterDays(interval),
                    lastReviewed = nowIso
                ).also {
                    showToast("Remembered → next review in $intervalStr")
                }
            }
        }

        viewModelScope.launch {
            repository.updateCard(updatedCard)
            // Advance to next card
            val currentList = studyCards.value
            val currentIndex = currentList.indexOfFirst { it.id == cardId }
            if (currentIndex >= 0 && currentList.size > 1) {
                _currentCardIndex.value = if (currentIndex < currentList.size - 1) currentIndex + 1 else 0
            } else {
                _currentCardIndex.value = 0
            }
            _isAnswerRevealed.value = false
            ttsHelper.stop()
        }
    }

    // Speed Review Toggle
    fun toggleSpeedReview() {
        _isSpeedReviewActive.value = !_isSpeedReviewActive.value
        if (_isSpeedReviewActive.value) {
            showToast("Speed review timer activated (10s per card)")
        }
    }

    // TTS Actions
    fun speakText(text: String) {
        ttsHelper.speak(text)
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    // Quiz Mode Logic
    fun initQuiz() {
        val deckCards = activeFolderCards.value
        if (deckCards.size < 2) {
            _quizQuestions.value = emptyList()
            _isQuizCompleted.value = false
            return
        }

        val questions = deckCards.shuffled().map { targetCard ->
            val otherAnswers = deckCards
                .filter { it.id != targetCard.id }
                .map { it.answer }
                .distinct()
                .shuffled()
                .take(3)

            val options = (otherAnswers + targetCard.answer).shuffled()
            QuizQuestion(
                cardId = targetCard.id,
                question = targetCard.question,
                correctAnswer = targetCard.answer,
                options = options,
                imageUri = targetCard.imageUri
            )
        }

        _quizQuestions.value = questions
        _currentQuizIndex.value = 0
        _selectedQuizAnswer.value = null
        _isQuizAnswerSubmitted.value = false
        _quizScore.value = 0
        _isQuizCompleted.value = false
    }

    fun selectQuizAnswer(answer: String) {
        if (!_isQuizAnswerSubmitted.value) {
            _selectedQuizAnswer.value = answer
        }
    }

    fun submitQuizAnswer() {
        val selected = _selectedQuizAnswer.value ?: return
        val currentQ = _quizQuestions.value.getOrNull(_currentQuizIndex.value) ?: return

        _isQuizAnswerSubmitted.value = true
        if (selected == currentQ.correctAnswer) {
            _quizScore.value += 1
            showToast("Correct!")
            // Reinforce card memory
            viewModelScope.launch {
                val card = allCards.value.find { it.id == currentQ.cardId }
                if (card != null) {
                    val step = card.reviewStep.coerceIn(0, BrainCardRepository.REVIEW_INTERVALS.size - 1)
                    val nextInterval = BrainCardRepository.REVIEW_INTERVALS[step]
                    repository.updateCard(
                        card.copy(
                            reviewStep = (step + 1).coerceAtMost(BrainCardRepository.REVIEW_INTERVALS.size - 1),
                            dueDate = BrainCardRepository.getDateStringAfterDays(nextInterval)
                        )
                    )
                }
            }
        } else {
            showToast("Incorrect! Right answer: ${currentQ.correctAnswer.take(40)}...")
        }
    }

    fun nextQuizQuestion() {
        val questions = _quizQuestions.value
        if (_currentQuizIndex.value < questions.size - 1) {
            _currentQuizIndex.value += 1
            _selectedQuizAnswer.value = null
            _isQuizAnswerSubmitted.value = false
        } else {
            _isQuizCompleted.value = true
        }
    }

    fun restartQuiz() {
        initQuiz()
    }

    // Add / Edit Card
    fun openAddCardDialog() {
        _showAddCardDialog.value = true
    }

    fun closeAddCardDialog() {
        _showAddCardDialog.value = false
    }

    fun openEditCardDialog(card: FlashcardEntity) {
        _editingCard.value = card
    }

    fun closeEditCardDialog() {
        _editingCard.value = null
    }

    fun addCard(question: String, answer: String, tags: String, imageUri: String? = null, folderId: String = activeFolderId.value) {
        val q = question.trim()
        val a = answer.trim()
        if (q.isBlank() || a.isBlank()) return

        viewModelScope.launch {
            repository.insertCard(
                folderId = folderId,
                question = q,
                answer = a,
                tags = tags.trim(),
                imageUri = imageUri
            )
            closeAddCardDialog()
            showToast("Card added")
            _currentCardIndex.value = studyCards.value.size
            _isAnswerRevealed.value = false
        }
    }

    fun updateCard(cardId: String, question: String, answer: String, tags: String, imageUri: String? = null) {
        val original = allCards.value.find { it.id == cardId } ?: return
        val q = question.trim()
        val a = answer.trim()
        if (q.isBlank() || a.isBlank()) return

        viewModelScope.launch {
            repository.updateCard(
                original.copy(
                    question = q,
                    answer = a,
                    tags = tags.trim(),
                    imageUri = imageUri ?: original.imageUri
                )
            )
            closeEditCardDialog()
            showToast("Card updated")
        }
    }

    fun confirmDeleteCard(card: FlashcardEntity) {
        _cardToDelete.value = card
    }

    fun dismissDeleteCard() {
        _cardToDelete.value = null
    }

    fun executeDeleteCard() {
        val card = _cardToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteCard(card.id)
            _cardToDelete.value = null
            val remaining = studyCards.value.filter { it.id != card.id }
            _currentCardIndex.value = _currentCardIndex.value.coerceIn(0, (remaining.size - 1).coerceAtLeast(0))
            _isAnswerRevealed.value = false
            showToast("Card deleted")
        }
    }

    // Folder Actions
    fun openAddFolderDialog() {
        _showAddFolderDialog.value = true
    }

    fun closeAddFolderDialog() {
        _showAddFolderDialog.value = false
    }

    fun openEditFolderDialog(folder: FolderEntity) {
        _editingFolder.value = folder
    }

    fun closeEditFolderDialog() {
        _editingFolder.value = null
    }

    fun addFolder(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val duplicate = allFolders.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (duplicate) {
            showToast("A folder with that name already exists")
            return false
        }

        viewModelScope.launch {
            val newFolder = repository.insertFolder(trimmed)
            _activeFolderId.value = newFolder.id
            _currentCardIndex.value = 0
            closeAddFolderDialog()
            showToast("Folder created")
        }
        return true
    }

    fun updateFolder(folderId: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return false
        val folder = allFolders.value.find { it.id == folderId } ?: return false

        viewModelScope.launch {
            repository.updateFolder(folder.copy(name = trimmed))
            closeEditFolderDialog()
            showToast("Folder updated")
        }
        return true
    }

    fun confirmDeleteFolder(folder: FolderEntity) {
        if (folder.id == "general") {
            showToast("The General folder cannot be deleted")
            return
        }
        _folderToDelete.value = folder
    }

    fun dismissDeleteFolder() {
        _folderToDelete.value = null
    }

    fun executeDeleteFolder() {
        val folder = _folderToDelete.value ?: return
        viewModelScope.launch {
            repository.deleteFolder(folder.id)
            if (_activeFolderId.value == folder.id) {
                _activeFolderId.value = "general"
                _currentCardIndex.value = 0
            }
            _folderToDelete.value = null
            showToast("Folder deleted")
        }
    }

    // Calendar navigation
    fun selectCalendarDate(dateString: String) {
        _selectedCalendarDate.value = dateString
    }

    fun changeCalendarMonth(amount: Int) {
        val cal = Calendar.getInstance().apply {
            time = _calendarMonthDate.value.time
            add(Calendar.MONTH, amount)
        }
        _calendarMonthDate.value = cal
    }

    fun goToCalendarToday() {
        _calendarMonthDate.value = Calendar.getInstance()
        _selectedCalendarDate.value = BrainCardRepository.getTodayDateString()
    }

    // Reminder Scheduling Actions
    fun openReminderDialog() {
        _showReminderDialog.value = true
    }

    fun dismissReminderDialog() {
        _showReminderDialog.value = false
    }

    fun enableReminder(hour: Int, minute: Int) {
        DailyReminderReceiver.scheduleReminder(getApplication(), hour, minute)
        _isReminderEnabled.value = true
        _reminderTime.value = Pair(hour, minute)
        val timeStr = String.format(Locale.US, "%02d:%02d", hour, minute)
        showToast("Daily reminder set for $timeStr")
        dismissReminderDialog()
    }

    fun disableReminder() {
        DailyReminderReceiver.cancelReminder(getApplication())
        _isReminderEnabled.value = false
        showToast("Daily study reminders turned off")
        dismissReminderDialog()
    }

    fun testReminderNotification() {
        DailyReminderReceiver.triggerTestNotification(getApplication())
        showToast("Test study reminder notification sent!")
    }

    // CSV & Text Import Actions
    fun openImportDialog() {
        _showImportDialog.value = true
    }

    fun dismissImportDialog() {
        _showImportDialog.value = false
    }

    fun importCards(parsedCards: List<ParsedCard>, targetFolderId: String = activeFolderId.value): Int {
        if (parsedCards.isEmpty()) {
            showToast("No valid card rows found to import")
            return 0
        }

        val todayStr = BrainCardRepository.getTodayDateString()
        val entities = parsedCards.map { card ->
            FlashcardEntity(
                id = "card_${UUID.randomUUID().toString().take(8)}",
                folderId = targetFolderId,
                question = card.question,
                answer = card.answer,
                tags = card.tags,
                reviewStep = 0,
                dueDate = todayStr,
                lastReviewed = null,
                createdAt = System.currentTimeMillis()
            )
        }

        viewModelScope.launch {
            repository.insertCards(entities)
            showToast("Successfully imported ${entities.size} cards!")
            dismissImportDialog()
        }
        return entities.size
    }

    fun importCsvFromUri(context: Context, uri: Uri, targetFolderId: String = activeFolderId.value) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val parsed = CsvImportHelper.parseStream(inputStream)
                importCards(parsed, targetFolderId)
            }
        } catch (e: Exception) {
            showToast("Import error: ${e.localizedMessage}")
        }
    }

    // Toast
    private fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    // CSV Export Actions
    fun openExportProgressDialog() {
        _showExportProgressDialog.value = true
    }

    fun dismissExportProgressDialog() {
        _showExportProgressDialog.value = false
    }

    fun exportProgressCsv(exportAllFolders: Boolean = false): String {
        return CsvExportHelper.generateProgressCsv(
            folders = allFolders.value,
            cards = allCards.value,
            activeFolderId = _activeFolderId.value,
            exportAllFolders = exportAllFolders
        )
    }

    fun shareCsv(context: Context, exportAllFolders: Boolean = false) {
        try {
            val csvContent = exportProgressCsv(exportAllFolders)
            val cacheFile = File(context.cacheDir, "braincard_study_progress.csv")
            cacheFile.writeText(csvContent, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "BrainCard Study Progress Export")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, csvContent)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Export Study Progress CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            showToast("Sharing study progress CSV...")
        } catch (e: Exception) {
            showToast("Export failed: ${e.localizedMessage}")
        }
    }

    fun writeCsvToUri(context: Context, uri: Uri, exportAllFolders: Boolean = false): Boolean {
        return try {
            val csvContent = exportProgressCsv(exportAllFolders)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            showToast("Study progress saved to CSV successfully!")
            true
        } catch (e: Exception) {
            showToast("Failed to save CSV: ${e.localizedMessage}")
            false
        }
    }

    fun copyCsvToClipboard(context: Context, exportAllFolders: Boolean = false) {
        try {
            val csvContent = exportProgressCsv(exportAllFolders)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("BrainCard Study Progress", csvContent)
            clipboard.setPrimaryClip(clip)
            showToast("Study progress CSV copied to clipboard!")
        } catch (e: Exception) {
            showToast("Failed to copy CSV: ${e.localizedMessage}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}
