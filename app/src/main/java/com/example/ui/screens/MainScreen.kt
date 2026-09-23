package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BrainCardTab
import com.example.ui.BrainCardViewModel
import com.example.ui.components.AddCardFormSection
import com.example.ui.components.AddFolderDialog
import com.example.ui.components.CardsLibrarySection
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EditCardDialog
import com.example.ui.components.EditFolderDialog
import com.example.ui.components.ExportProgressDialog
import com.example.ui.components.FlashcardPlayer
import com.example.ui.components.FoldersSection
import com.example.ui.components.HeaderBar
import com.example.ui.components.ImportCardsDialog
import com.example.ui.components.QuizSection
import com.example.ui.components.ReminderSettingsDialog
import com.example.ui.components.ReviewCalendar
import com.example.ui.components.SummaryCards
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: BrainCardViewModel,
    modifier: Modifier = Modifier
) {
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val activeFolderCards by viewModel.activeFolderCards.collectAsStateWithLifecycle()
    val studyCards by viewModel.studyCards.collectAsStateWithLifecycle()
    val activeFolderId by viewModel.activeFolderId.collectAsStateWithLifecycle()
    val currentCardIndex by viewModel.currentCardIndex.collectAsStateWithLifecycle()
    val isAnswerRevealed by viewModel.isAnswerRevealed.collectAsStateWithLifecycle()
    val isDarkThemeOverride by viewModel.isDarkThemeOverride.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()
    val calendarMonthDate by viewModel.calendarMonthDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val dueCount by viewModel.dueCardsCount.collectAsStateWithLifecycle()
    val tomorrowCount by viewModel.tomorrowCardsCount.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCardsCount.collectAsStateWithLifecycle()

    // Reminders & Speed mode
    val isReminderEnabled by viewModel.isReminderEnabled.collectAsStateWithLifecycle()
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle()
    val isSpeedReviewActive by viewModel.isSpeedReviewActive.collectAsStateWithLifecycle()

    // Quiz state
    val quizQuestions by viewModel.quizQuestions.collectAsStateWithLifecycle()
    val currentQuizIndex by viewModel.currentQuizIndex.collectAsStateWithLifecycle()
    val selectedQuizAnswer by viewModel.selectedQuizAnswer.collectAsStateWithLifecycle()
    val isQuizAnswerSubmitted by viewModel.isQuizAnswerSubmitted.collectAsStateWithLifecycle()
    val quizScore by viewModel.quizScore.collectAsStateWithLifecycle()
    val isQuizCompleted by viewModel.isQuizCompleted.collectAsStateWithLifecycle()

    // Dialogs state
    val showAddFolderDialog by viewModel.showAddFolderDialog.collectAsStateWithLifecycle()
    val editingFolder by viewModel.editingFolder.collectAsStateWithLifecycle()
    val editingCard by viewModel.editingCard.collectAsStateWithLifecycle()
    val folderToDelete by viewModel.folderToDelete.collectAsStateWithLifecycle()
    val cardToDelete by viewModel.cardToDelete.collectAsStateWithLifecycle()
    val showExportProgressDialog by viewModel.showExportProgressDialog.collectAsStateWithLifecycle()
    val showImportDialog by viewModel.showImportDialog.collectAsStateWithLifecycle()
    val showReminderDialog by viewModel.showReminderDialog.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var pendingExportAll by remember { mutableStateOf(false) }

    // Export launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.writeCsvToUri(context, uri, pendingExportAll)
        }
    }

    // Import file picker launcher
    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCsvFromUri(context, uri)
        }
    }

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openReminderDialog()
        } else {
            // Still allow configuring
            viewModel.openReminderDialog()
        }
    }

    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = isDarkThemeOverride ?: systemDark

    // Auto-dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2400)
            viewModel.dismissToast()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == BrainCardTab.STUDY,
                    onClick = { viewModel.setActiveTab(BrainCardTab.STUDY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (dueCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = if (dueCount > 99) "99+" else dueCount.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Study"
                            )
                        }
                    },
                    label = { Text("Study", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_study")
                )

                NavigationBarItem(
                    selected = activeTab == BrainCardTab.QUIZ,
                    onClick = { viewModel.setActiveTab(BrainCardTab.QUIZ) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = "Quiz"
                        )
                    },
                    label = { Text("Quiz", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_quiz")
                )

                NavigationBarItem(
                    selected = activeTab == BrainCardTab.CALENDAR,
                    onClick = { viewModel.setActiveTab(BrainCardTab.CALENDAR) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar"
                        )
                    },
                    label = { Text("Calendar", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_calendar")
                )

                NavigationBarItem(
                    selected = activeTab == BrainCardTab.LIBRARY,
                    onClick = { viewModel.setActiveTab(BrainCardTab.LIBRARY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FolderCopy,
                            contentDescription = "Cards"
                        )
                    },
                    label = { Text("Cards", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_library")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .widthIn(max = 700.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Header Bar (Brand + Import + Reminders + Dark Mode)
                HeaderBar(
                    isDarkTheme = isDarkTheme,
                    isReminderEnabled = isReminderEnabled,
                    onToggleTheme = { viewModel.toggleTheme(isDarkTheme) },
                    onOpenReminders = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.openReminderDialog()
                        }
                    },
                    onOpenImport = { viewModel.openImportDialog() }
                )

                // Folders Bar
                FoldersSection(
                    folders = folders,
                    cards = allCards,
                    activeFolderId = activeFolderId,
                    onSelectFolder = { viewModel.selectFolder(it) },
                    onAddFolderClick = { viewModel.openAddFolderDialog() },
                    onEditFolderClick = { viewModel.openEditFolderDialog(it) },
                    onDeleteFolderClick = { viewModel.confirmDeleteFolder(it) }
                )

                // Summary 3-Pack Grid
                SummaryCards(
                    dueCount = dueCount,
                    tomorrowCount = tomorrowCount,
                    totalCount = totalCount,
                    onExportCsvClick = { viewModel.openExportProgressDialog() }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tab Content
                when (activeTab) {
                    BrainCardTab.STUDY -> {
                        // Study Flashcard Player with TTS and Speed Review
                        FlashcardPlayer(
                            cards = studyCards,
                            currentIndex = currentCardIndex,
                            isAnswerRevealed = isAnswerRevealed,
                            isSpeedReview = isSpeedReviewActive,
                            onToggleSpeedReview = { viewModel.toggleSpeedReview() },
                            onSpeak = { viewModel.speakText(it) },
                            onRevealAnswer = { viewModel.revealAnswer() },
                            onReviewResult = { id, result -> viewModel.handleReview(id, result) },
                            onNextCard = { viewModel.nextCard() },
                            onPreviousCard = { viewModel.previousCard() },
                            onEditCard = { viewModel.openEditCardDialog(it) },
                            onDeleteCard = { viewModel.confirmDeleteCard(it) },
                            onAddCardClick = { viewModel.openAddCardDialog() }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Review Calendar Section in Study tab
                        ReviewCalendar(
                            cards = activeFolderCards,
                            selectedDate = selectedCalendarDate,
                            calendarMonth = calendarMonthDate,
                            onDateSelected = { viewModel.selectCalendarDate(it) },
                            onChangeMonth = { viewModel.changeCalendarMonth(it) },
                            onGoToToday = { viewModel.goToCalendarToday() },
                            onStudyCard = { viewModel.studySpecificCard(it) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add Card Section
                        AddCardFormSection(
                            onAddCard = { q, a, t, img -> viewModel.addCard(q, a, t, img) }
                        )
                    }

                    BrainCardTab.QUIZ -> {
                        // Multiple Choice Quiz Mode
                        QuizSection(
                            questions = quizQuestions,
                            currentIndex = currentQuizIndex,
                            selectedAnswer = selectedQuizAnswer,
                            isAnswerSubmitted = isQuizAnswerSubmitted,
                            score = quizScore,
                            isQuizCompleted = isQuizCompleted,
                            onSelectAnswer = { viewModel.selectQuizAnswer(it) },
                            onSubmitAnswer = { viewModel.submitQuizAnswer() },
                            onNextQuestion = { viewModel.nextQuizQuestion() },
                            onRestartQuiz = { viewModel.restartQuiz() },
                            onSpeak = { viewModel.speakText(it) }
                        )
                    }

                    BrainCardTab.CALENDAR -> {
                        // Full Calendar View
                        ReviewCalendar(
                            cards = activeFolderCards,
                            selectedDate = selectedCalendarDate,
                            calendarMonth = calendarMonthDate,
                            onDateSelected = { viewModel.selectCalendarDate(it) },
                            onChangeMonth = { viewModel.changeCalendarMonth(it) },
                            onGoToToday = { viewModel.goToCalendarToday() },
                            onStudyCard = { viewModel.studySpecificCard(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Add Card Form below calendar
                        AddCardFormSection(
                            onAddCard = { q, a, t, img -> viewModel.addCard(q, a, t, img) }
                        )
                    }

                    BrainCardTab.LIBRARY -> {
                        // Searchable & Filterable Library
                        CardsLibrarySection(
                            cards = activeFolderCards,
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onStudyCard = { viewModel.studySpecificCard(it) },
                            onEditCard = { viewModel.openEditCardDialog(it) },
                            onDeleteCard = { viewModel.confirmDeleteCard(it) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Add Card Form
                        AddCardFormSection(
                            onAddCard = { q, a, t, img -> viewModel.addCard(q, a, t, img) }
                        )
                    }

                    BrainCardTab.STATS -> {
                        // Reserved for future stats
                    }
                }

                // Footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BrainCard · Abdullah Al Mamun",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Toast Alert matching the modern UI
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 60 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 60 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    shadowElevation = 8.dp,
                    modifier = Modifier.testTag("app_toast")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = toastMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddFolderDialog) {
        AddFolderDialog(
            onDismiss = { viewModel.closeAddFolderDialog() },
            onSave = { viewModel.addFolder(it) }
        )
    }

    editingFolder?.let { folder ->
        EditFolderDialog(
            folder = folder,
            onDismiss = { viewModel.closeEditFolderDialog() },
            onSave = { newName -> viewModel.updateFolder(folder.id, newName) }
        )
    }

    folderToDelete?.let { folder ->
        ConfirmDeleteDialog(
            title = "Delete Deck",
            message = "Are you sure you want to delete folder \"${folder.name}\"? Cards inside this folder will also be removed.",
            onConfirm = { viewModel.executeDeleteFolder() },
            onDismiss = { viewModel.dismissDeleteFolder() }
        )
    }

    editingCard?.let { card ->
        EditCardDialog(
            card = card,
            onDismiss = { viewModel.closeEditCardDialog() },
            onSave = { q, a, t, img -> viewModel.updateCard(card.id, q, a, t, img) }
        )
    }

    cardToDelete?.let { card ->
        ConfirmDeleteDialog(
            title = "Delete Flashcard",
            message = "Are you sure you want to delete this card? This action cannot be undone.",
            onConfirm = { viewModel.executeDeleteCard() },
            onDismiss = { viewModel.dismissDeleteCard() }
        )
    }

    if (showExportProgressDialog) {
        ExportProgressDialog(
            folders = folders,
            cards = allCards,
            activeFolderId = activeFolderId,
            onDismiss = { viewModel.dismissExportProgressDialog() },
            onSaveToFile = { exportAll ->
                pendingExportAll = exportAll
                val prefix = if (exportAll) "braincard_all_decks" else "braincard_${activeFolderId}"
                createDocumentLauncher.launch("${prefix}_progress.csv")
            },
            onShare = { exportAll ->
                viewModel.shareCsv(context, exportAll)
            },
            onCopy = { exportAll ->
                viewModel.copyCsvToClipboard(context, exportAll)
            }
        )
    }

    if (showImportDialog) {
        ImportCardsDialog(
            folders = folders,
            activeFolderId = activeFolderId,
            onDismiss = { viewModel.dismissImportDialog() },
            onImportCards = { parsedList, targetFolder ->
                viewModel.importCards(parsedList, targetFolder)
            },
            onPickFile = {
                importFilePicker.launch(arrayOf("text/*", "application/vnd.ms-excel", "*/*"))
            }
        )
    }

    if (showReminderDialog) {
        ReminderSettingsDialog(
            isEnabled = isReminderEnabled,
            currentTime = reminderTime,
            onDismiss = { viewModel.dismissReminderDialog() },
            onSaveReminder = { hour, minute ->
                viewModel.enableReminder(hour, minute)
            },
            onDisableReminder = {
                viewModel.disableReminder()
            },
            onTestNotification = {
                viewModel.testReminderNotification()
            }
        )
    }
}
