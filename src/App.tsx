import React, { useState, useEffect } from 'react';
import {
  Folder,
  Flashcard,
  REVIEW_INTERVALS,
  getTodayDateString,
  getDateStringAfterDays,
  formatReadableDate
} from './types';
import {
  loadStoredFolders,
  saveStoredFolders,
  loadStoredCards,
  saveStoredCards
} from './storage';
import {
  BookOpen,
  Calendar as CalendarIcon,
  Layers,
  HelpCircle,
  BarChart3,
  Plus,
  Volume2,
  RotateCw,
  CheckCircle,
  XCircle,
  FolderPlus,
  Download,
  Upload,
  Sun,
  Moon,
  Trash2,
  Edit2,
  Search,
  Check,
  ChevronLeft,
  ChevronRight,
  Share2
} from 'lucide-react';

type Tab = 'study' | 'quiz' | 'calendar' | 'library' | 'stats';

export default function App() {
  const [folders, setFolders] = useState<Folder[]>(loadStoredFolders);
  const [cards, setCards] = useState<Flashcard[]>(loadStoredCards);
  const [activeFolderId, setActiveFolderId] = useState<string>('general');
  const [activeTab, setActiveTab] = useState<Tab>('study');
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');

  // Study state
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Modals
  const [showAddCard, setShowAddCard] = useState(false);
  const [showAddFolder, setShowAddFolder] = useState(false);
  const [editingCard, setEditingCard] = useState<Flashcard | null>(null);

  // Form states
  const [formQuestion, setFormQuestion] = useState('');
  const [formAnswer, setFormAnswer] = useState('');
  const [formTags, setFormTags] = useState('');
  const [newFolderName, setNewFolderName] = useState('');

  // Quiz State
  const [quizQuestions, setQuizQuestions] = useState<any[]>([]);
  const [quizIndex, setQuizIndex] = useState(0);
  const [quizSelected, setQuizSelected] = useState<string | null>(null);
  const [quizScore, setQuizScore] = useState(0);
  const [quizFinished, setQuizFinished] = useState(false);

  // Calendar State
  const [calendarDate, setCalendarDate] = useState(getTodayDateString());

  // Save changes
  useEffect(() => {
    saveStoredFolders(folders);
  }, [folders]);

  useEffect(() => {
    saveStoredCards(cards);
  }, [cards]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const activeFolderCards = cards.filter(c => c.folderId === activeFolderId);
  const todayStr = getTodayDateString();

  const studyCards = [...activeFolderCards].sort((a, b) => {
    const aDue = a.dueDate <= todayStr ? 0 : 1;
    const bDue = b.dueDate <= todayStr ? 0 : 1;
    if (aDue !== bDue) return aDue - bDue;
    return a.dueDate.localeCompare(b.dueDate);
  });

  const dueCardsCount = activeFolderCards.filter(c => c.dueDate <= todayStr).length;
  const currentCard = studyCards[currentCardIndex] || null;

  // Review handler (Spaced Repetition)
  const handleReview = (result: 'forgot' | 'remembered') => {
    if (!currentCard) return;
    const nowIso = new Date().toISOString();

    let nextStep = 0;
    let nextDueDate = todayStr;

    if (result === 'forgot') {
      nextStep = 0;
      nextDueDate = getDateStringAfterDays(1);
      showToast('Forgot → next review in 1 day');
    } else {
      const step = Math.min(Math.max(currentCard.reviewStep, 0), REVIEW_INTERVALS.length - 1);
      const interval = REVIEW_INTERVALS[step];
      nextStep = Math.min(step + 1, REVIEW_INTERVALS.length - 1);
      nextDueDate = getDateStringAfterDays(interval);
      showToast(`Remembered → next review in ${interval} day${interval > 1 ? 's' : ''}`);
    }

    const updated = cards.map(c =>
      c.id === currentCard.id
        ? { ...c, reviewStep: nextStep, dueDate: nextDueDate, lastReviewed: nowIso }
        : c
    );
    setCards(updated);
    setIsFlipped(false);

    if (studyCards.length > 1) {
      setCurrentCardIndex((currentCardIndex + 1) % studyCards.length);
    }
  };

  // Text to Speech
  const speakText = (text: string) => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = 0.95;
      window.speechSynthesis.speak(utterance);
    }
  };

  // Card CRUD
  const saveCard = () => {
    if (!formQuestion.trim() || !formAnswer.trim()) {
      showToast('Please enter both question and answer');
      return;
    }

    if (editingCard) {
      setCards(cards.map(c => c.id === editingCard.id ? {
        ...c,
        question: formQuestion.trim(),
        answer: formAnswer.trim(),
        tags: formTags.trim()
      } : c));
      showToast('Card updated!');
    } else {
      const newCard: Flashcard = {
        id: `card_${Date.now()}`,
        folderId: activeFolderId,
        question: formQuestion.trim(),
        answer: formAnswer.trim(),
        tags: formTags.trim(),
        reviewStep: 0,
        dueDate: todayStr,
        lastReviewed: null,
        createdAt: Date.now()
      };
      setCards([...cards, newCard]);
      showToast('Card added!');
    }
    closeCardModal();
  };

  const deleteCard = (id: string) => {
    setCards(cards.filter(c => c.id !== id));
    showToast('Card deleted');
    if (currentCardIndex >= studyCards.length - 1) {
      setCurrentCardIndex(Math.max(0, studyCards.length - 2));
    }
  };

  const openCardModal = (card?: Flashcard) => {
    if (card) {
      setEditingCard(card);
      setFormQuestion(card.question);
      setFormAnswer(card.answer);
      setFormTags(card.tags);
    } else {
      setEditingCard(null);
      setFormQuestion('');
      setFormAnswer('');
      setFormTags('');
    }
    setShowAddCard(true);
  };

  const closeCardModal = () => {
    setShowAddCard(false);
    setEditingCard(null);
    setFormQuestion('');
    setFormAnswer('');
    setFormTags('');
  };

  // Folder CRUD
  const addFolder = () => {
    const name = newFolderName.trim();
    if (!name) return;
    if (folders.some(f => f.name.toLowerCase() === name.toLowerCase())) {
      showToast('A folder with that name already exists');
      return;
    }
    const newFolder: Folder = {
      id: `folder_${Date.now()}`,
      name,
      createdAt: Date.now()
    };
    setFolders([...folders, newFolder]);
    setActiveFolderId(newFolder.id);
    setNewFolderName('');
    setShowAddFolder(false);
    showToast('Folder created!');
  };

  const deleteFolder = (id: string) => {
    if (id === 'general') {
      showToast('Cannot delete General folder');
      return;
    }
    setFolders(folders.filter(f => f.id !== id));
    setCards(cards.filter(c => c.folderId !== id));
    setActiveFolderId('general');
    showToast('Folder deleted');
  };

  // Quiz init
  const startQuiz = () => {
    const deck = activeFolderCards;
    if (deck.length < 2) {
      showToast('Need at least 2 cards in folder to start quiz');
      return;
    }
    const shuffled = [...deck].sort(() => Math.random() - 0.5).map(c => {
      const otherAnswers = deck
        .filter(other => other.id !== c.id)
        .map(other => other.answer)
        .sort(() => Math.random() - 0.5)
        .slice(0, 3);
      const options = [...otherAnswers, c.answer].sort(() => Math.random() - 0.5);
      return {
        id: c.id,
        question: c.question,
        correctAnswer: c.answer,
        options
      };
    });
    setQuizQuestions(shuffled);
    setQuizIndex(0);
    setQuizSelected(null);
    setQuizScore(0);
    setQuizFinished(false);
    setActiveTab('quiz');
  };

  // CSV Export
  const exportCSV = () => {
    const headers = 'ID,Folder,Question,Answer,Tags,ReviewStep,DueDate,LastReviewed\n';
    const rows = cards.map(c => {
      const folderName = folders.find(f => f.id === c.folderId)?.name || 'General';
      const escape = (str: string) => `"${(str || '').replace(/"/g, '""')}"`;
      return `${c.id},${escape(folderName)},${escape(c.question)},${escape(c.answer)},${escape(c.tags)},${c.reviewStep},${c.dueDate},${c.lastReviewed || ''}`;
    }).join('\n');

    const blob = new Blob([headers + rows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `braincard_study_export_${todayStr}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast('Exported CSV successfully!');
  };

  // CSV Import
  const handleImportFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const text = event.target?.result as string;
      if (!text) return;
      const lines = text.split(/\r?\n/).filter(line => line.trim().length > 0);
      let count = 0;
      const newImported: Flashcard[] = [];

      for (let i = 1; i < lines.length; i++) {
        const parts = lines[i].split(',');
        if (parts.length >= 4) {
          newImported.push({
            id: `card_${Date.now()}_${i}`,
            folderId: activeFolderId,
            question: parts[2].replace(/^"|"$/g, '').trim(),
            answer: parts[3].replace(/^"|"$/g, '').trim(),
            tags: parts[4] ? parts[4].replace(/^"|"$/g, '').trim() : '',
            reviewStep: 0,
            dueDate: todayStr,
            lastReviewed: null,
            createdAt: Date.now()
          });
          count++;
        }
      }
      if (newImported.length > 0) {
        setCards([...cards, ...newImported]);
        showToast(`Imported ${count} flashcards!`);
      } else {
        showToast('No valid card rows found in CSV');
      }
    };
    reader.readAsText(file);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', maxWidth: 840, margin: '0 auto', width: '100%' }}>
      {/* Toast */}
      {toastMessage && (
        <div style={{
          position: 'fixed',
          bottom: 84,
          left: '50%',
          transform: 'translateX(-50%)',
          backgroundColor: 'var(--accent-primary)',
          color: '#ffffff',
          padding: '10px 20px',
          borderRadius: '999px',
          fontWeight: 600,
          fontSize: '0.875rem',
          boxShadow: 'var(--shadow)',
          zIndex: 1000,
          animation: 'fadeIn 0.2s ease'
        }}>
          {toastMessage}
        </div>
      )}

      {/* Header */}
      <header style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '16px 20px',
        borderBottom: '1px solid var(--border-color)',
        backgroundColor: 'var(--bg-secondary)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{
            width: 38,
            height: 38,
            borderRadius: 'var(--radius-sm)',
            backgroundColor: 'var(--accent-primary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            fontWeight: 800,
            fontSize: '1.2rem'
          }}>
            🧠
          </div>
          <div>
            <h1 style={{ fontSize: '1.15rem', fontWeight: 800, letterSpacing: '-0.02em' }}>BrainCard PWA</h1>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Spaced Repetition Study</p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button
            onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
            style={{
              padding: 8,
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border-color)',
              color: 'var(--text-secondary)'
            }}
            title="Toggle Dark/Light mode"
          >
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>
          <button
            onClick={() => openCardModal()}
            style={{
              backgroundColor: 'var(--accent-primary)',
              color: '#ffffff',
              padding: '8px 14px',
              borderRadius: 'var(--radius-sm)',
              fontWeight: 600,
              fontSize: '0.85rem',
              gap: 6
            }}
          >
            <Plus size={16} /> New Card
          </button>
        </div>
      </header>

      {/* Folder selector bar */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        padding: '10px 16px',
        gap: 8,
        overflowX: 'auto',
        backgroundColor: 'var(--bg-primary)',
        borderBottom: '1px solid var(--border-color)',
        scrollbarWidth: 'none'
      }}>
        {folders.map(folder => (
          <button
            key={folder.id}
            onClick={() => {
              setActiveFolderId(folder.id);
              setCurrentCardIndex(0);
              setIsFlipped(false);
            }}
            style={{
              padding: '6px 14px',
              borderRadius: '999px',
              fontSize: '0.825rem',
              fontWeight: 600,
              whiteSpace: 'nowrap',
              backgroundColor: activeFolderId === folder.id ? 'var(--accent-primary)' : 'var(--bg-secondary)',
              color: activeFolderId === folder.id ? '#ffffff' : 'var(--text-secondary)',
              border: '1px solid',
              borderColor: activeFolderId === folder.id ? 'var(--accent-primary)' : varToString('--border-color')
            }}
          >
            📁 {folder.name} ({cards.filter(c => c.folderId === folder.id).length})
          </button>
        ))}
        <button
          onClick={() => setShowAddFolder(true)}
          style={{
            padding: '6px 12px',
            borderRadius: '999px',
            fontSize: '0.825rem',
            color: 'var(--accent-primary)',
            backgroundColor: 'var(--bg-secondary)',
            border: '1px dashed var(--accent-primary)',
            whiteSpace: 'nowrap',
            gap: 4
          }}
        >
          <FolderPlus size={14} /> New Folder
        </button>
      </div>

      {/* Main content body */}
      <main style={{ flex: 1, padding: '16px 20px', display: 'flex', flexDirection: 'column' }}>
        {/* Tab 1: STUDY */}
        {activeTab === 'study' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16, flex: 1 }}>
            {/* Folder stats pills */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 10 }}>
              <div style={{
                backgroundColor: 'var(--bg-secondary)',
                padding: '12px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--danger)' }}>{dueCardsCount}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Due Today</div>
              </div>
              <div style={{
                backgroundColor: 'var(--bg-secondary)',
                padding: '12px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--accent-primary)' }}>{studyCards.length}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Deck Total</div>
              </div>
              <div style={{
                backgroundColor: 'var(--bg-secondary)',
                padding: '12px 14px',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-color)',
                textAlign: 'center',
                cursor: 'pointer'
              }} onClick={startQuiz}>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--success)' }}>⚡ Quiz</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Start Mode</div>
              </div>
            </div>

            {/* Flashcard Player */}
            {studyCards.length > 0 && currentCard ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  <span>Card {currentCardIndex + 1} of {studyCards.length}</span>
                  <span>Interval Step: Level {currentCard.reviewStep + 1} / 5</span>
                </div>

                {/* Flip Card */}
                <div
                  onClick={() => setIsFlipped(!isFlipped)}
                  style={{
                    minHeight: 280,
                    backgroundColor: 'var(--bg-card)',
                    border: '1px solid var(--border-color)',
                    borderRadius: 'var(--radius-lg)',
                    padding: '24px 28px',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    boxShadow: 'var(--shadow)',
                    cursor: 'pointer',
                    transition: 'transform 0.2s ease, border-color 0.2s',
                    position: 'relative'
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <span style={{
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      color: isFlipped ? 'var(--success)' : 'var(--accent-primary)',
                      backgroundColor: isFlipped ? 'var(--success-bg)' : 'rgba(99, 102, 241, 0.15)',
                      padding: '4px 10px',
                      borderRadius: 999
                    }}>
                      {isFlipped ? 'Answer Side' : 'Question Side'}
                    </span>

                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        speakText(isFlipped ? currentCard.answer : currentCard.question);
                      }}
                      style={{
                        padding: 6,
                        borderRadius: '50%',
                        backgroundColor: 'var(--bg-secondary)',
                        color: 'var(--text-secondary)'
                      }}
                      title="Listen with TTS speech"
                    >
                      <Volume2 size={18} />
                    </button>
                  </div>

                  <div style={{ margin: '24px 0', textAlign: 'center' }}>
                    <p style={{
                      fontSize: '1.25rem',
                      fontWeight: 600,
                      lineHeight: 1.5,
                      color: 'var(--text-primary)',
                      whiteSpace: 'pre-wrap'
                    }}>
                      {isFlipped ? currentCard.answer : currentCard.question}
                    </p>
                    {!isFlipped && (
                      <p style={{ marginTop: 16, fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        (Tap anywhere to reveal answer)
                      </p>
                    )}
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      Due: {formatReadableDate(currentCard.dueDate)}
                    </span>
                    {currentCard.tags && (
                      <span style={{ fontSize: '0.75rem', color: 'var(--accent-primary)' }}>
                        #{currentCard.tags}
                      </span>
                    )}
                  </div>
                </div>

                {/* Review action buttons */}
                {isFlipped ? (
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                    <button
                      onClick={() => handleReview('forgot')}
                      style={{
                        padding: '14px',
                        backgroundColor: 'var(--danger-bg)',
                        color: 'var(--danger)',
                        border: '1px solid var(--danger)',
                        borderRadius: 'var(--radius-md)',
                        fontWeight: 700,
                        gap: 8
                      }}
                    >
                      <XCircle size={20} /> Forgot (1d)
                    </button>
                    <button
                      onClick={() => handleReview('remembered')}
                      style={{
                        padding: '14px',
                        backgroundColor: 'var(--success-bg)',
                        color: 'var(--success)',
                        border: '1px solid var(--success)',
                        borderRadius: 'var(--radius-md)',
                        fontWeight: 700,
                        gap: 8
                      }}
                    >
                      <CheckCircle size={20} /> Remembered
                    </button>
                  </div>
                ) : (
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10 }}>
                    <button
                      onClick={() => {
                        setCurrentCardIndex((currentCardIndex - 1 + studyCards.length) % studyCards.length);
                        setIsFlipped(false);
                      }}
                      style={{
                        flex: 1,
                        padding: 12,
                        backgroundColor: 'var(--bg-secondary)',
                        border: '1px solid var(--border-color)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--text-secondary)',
                        gap: 6
                      }}
                    >
                      <ChevronLeft size={18} /> Prev
                    </button>
                    <button
                      onClick={() => setIsFlipped(true)}
                      style={{
                        flex: 2,
                        padding: 12,
                        backgroundColor: 'var(--accent-primary)',
                        color: '#ffffff',
                        borderRadius: 'var(--radius-md)',
                        fontWeight: 700,
                        gap: 6
                      }}
                    >
                      <RotateCw size={18} /> Flip Card
                    </button>
                    <button
                      onClick={() => {
                        setCurrentCardIndex((currentCardIndex + 1) % studyCards.length);
                        setIsFlipped(false);
                      }}
                      style={{
                        flex: 1,
                        padding: 12,
                        backgroundColor: 'var(--bg-secondary)',
                        border: '1px solid var(--border-color)',
                        borderRadius: 'var(--radius-md)',
                        color: 'var(--text-secondary)',
                        gap: 6
                      }}
                    >
                      Next <ChevronRight size={18} />
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div style={{
                padding: '48px 20px',
                textAlign: 'center',
                backgroundColor: 'var(--bg-secondary)',
                borderRadius: 'var(--radius-lg)',
                border: '1px dashed var(--border-color)',
                marginTop: 20
              }}>
                <div style={{ fontSize: '2.5rem', marginBottom: 12 }}>🎉</div>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>No cards in this folder yet</h3>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: 4, marginBottom: 16 }}>
                  Create your first card or import study decks to begin!
                </p>
                <button
                  onClick={() => openCardModal()}
                  style={{
                    backgroundColor: 'var(--accent-primary)',
                    color: '#ffffff',
                    padding: '10px 18px',
                    borderRadius: 'var(--radius-sm)',
                    fontWeight: 600,
                    gap: 6
                  }}
                >
                  <Plus size={16} /> Add First Flashcard
                </button>
              </div>
            )}
          </div>
        )}

        {/* Tab 2: QUIZ */}
        {activeTab === 'quiz' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {quizQuestions.length > 0 && !quizFinished ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    Question {quizIndex + 1} of {quizQuestions.length}
                  </span>
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--success)' }}>
                    Score: {quizScore}
                  </span>
                </div>

                <div style={{
                  backgroundColor: 'var(--bg-card)',
                  padding: 24,
                  borderRadius: 'var(--radius-lg)',
                  border: '1px solid var(--border-color)'
                }}>
                  <p style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: 20 }}>
                    {quizQuestions[quizIndex]?.question}
                  </p>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                    {quizQuestions[quizIndex]?.options.map((opt: string, i: number) => {
                      const isSelected = quizSelected === opt;
                      const isSubmitted = quizSelected !== null;
                      const isCorrect = opt === quizQuestions[quizIndex].correctAnswer;

                      let btnBg = 'var(--bg-secondary)';
                      let btnBorder = 'var(--border-color)';
                      let btnColor = 'var(--text-primary)';

                      if (isSubmitted) {
                        if (isCorrect) {
                          btnBg = 'var(--success-bg)';
                          btnBorder = 'var(--success)';
                          btnColor = 'var(--success)';
                        } else if (isSelected && !isCorrect) {
                          btnBg = 'var(--danger-bg)';
                          btnBorder = 'var(--danger)';
                          btnColor = 'var(--danger)';
                        }
                      }

                      return (
                        <button
                          key={i}
                          disabled={quizSelected !== null}
                          onClick={() => {
                            setQuizSelected(opt);
                            if (opt === quizQuestions[quizIndex].correctAnswer) {
                              setQuizScore(quizScore + 1);
                              showToast('Correct! 🌟');
                            } else {
                              showToast('Incorrect!');
                            }
                          }}
                          style={{
                            padding: '14px 16px',
                            borderRadius: 'var(--radius-md)',
                            border: `1px solid ${btnBorder}`,
                            backgroundColor: btnBg,
                            color: btnColor,
                            textAlign: 'left',
                            justifyContent: 'flex-start',
                            fontWeight: 600,
                            fontSize: '0.95rem',
                            lineHeight: 1.4
                          }}
                        >
                          {opt}
                        </button>
                      );
                    })}
                  </div>

                  {quizSelected !== null && (
                    <button
                      onClick={() => {
                        if (quizIndex < quizQuestions.length - 1) {
                          setQuizIndex(quizIndex + 1);
                          setQuizSelected(null);
                        } else {
                          setQuizFinished(true);
                        }
                      }}
                      style={{
                        marginTop: 20,
                        width: '100%',
                        padding: 14,
                        backgroundColor: 'var(--accent-primary)',
                        color: '#ffffff',
                        borderRadius: 'var(--radius-md)',
                        fontWeight: 700
                      }}
                    >
                      {quizIndex < quizQuestions.length - 1 ? 'Next Question' : 'View Results'}
                    </button>
                  )}
                </div>
              </div>
            ) : quizFinished ? (
              <div style={{
                textAlign: 'center',
                padding: '40px 20px',
                backgroundColor: 'var(--bg-card)',
                borderRadius: 'var(--radius-lg)',
                border: '1px solid var(--border-color)'
              }}>
                <div style={{ fontSize: '3rem', marginBottom: 10 }}>🏆</div>
                <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>Quiz Completed!</h2>
                <p style={{ fontSize: '1.1rem', color: 'var(--text-secondary)', margin: '10px 0 24px' }}>
                  You got <strong style={{ color: 'var(--success)' }}>{quizScore}</strong> out of <strong>{quizQuestions.length}</strong> correct!
                </p>
                <div style={{ display: 'flex', justifyContent: 'center', gap: 12 }}>
                  <button
                    onClick={startQuiz}
                    style={{
                      padding: '12px 20px',
                      backgroundColor: 'var(--accent-primary)',
                      color: '#ffffff',
                      borderRadius: 'var(--radius-md)',
                      fontWeight: 700
                    }}
                  >
                    Try Again
                  </button>
                  <button
                    onClick={() => setActiveTab('study')}
                    style={{
                      padding: '12px 20px',
                      backgroundColor: 'var(--bg-secondary)',
                      border: '1px solid var(--border-color)',
                      color: 'var(--text-primary)',
                      borderRadius: 'var(--radius-md)',
                      fontWeight: 600
                    }}
                  >
                    Back to Study
                  </button>
                </div>
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '40px 20px' }}>
                <p style={{ color: 'var(--text-muted)', marginBottom: 14 }}>
                  Need at least 2 cards in the active folder to generate multiple-choice quiz questions.
                </p>
                <button
                  onClick={startQuiz}
                  style={{
                    backgroundColor: 'var(--accent-primary)',
                    color: '#ffffff',
                    padding: '10px 18px',
                    borderRadius: 'var(--radius-sm)',
                    fontWeight: 600
                  }}
                >
                  Start Quiz
                </button>
              </div>
            )}
          </div>
        )}

        {/* Tab 3: CALENDAR */}
        {activeTab === 'calendar' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <h2 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Review Schedule Heatmap</h2>
            <div style={{
              backgroundColor: 'var(--bg-card)',
              padding: 20,
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border-color)'
            }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 8 }}>
                {[...Array(21)].map((_, i) => {
                  const targetDate = getDateStringAfterDays(i);
                  const count = cards.filter(c => c.dueDate === targetDate).length;
                  const isSelected = calendarDate === targetDate;

                  return (
                    <div
                      key={i}
                      onClick={() => setCalendarDate(targetDate)}
                      style={{
                        padding: 10,
                        backgroundColor: isSelected ? 'var(--accent-primary)' : 'var(--bg-secondary)',
                        color: isSelected ? '#ffffff' : 'var(--text-primary)',
                        border: '1px solid var(--border-color)',
                        borderRadius: 'var(--radius-sm)',
                        textAlign: 'center',
                        cursor: 'pointer'
                      }}
                    >
                      <div style={{ fontSize: '0.75rem', color: isSelected ? '#ffffff' : 'var(--text-muted)' }}>
                        {formatReadableDate(targetDate).slice(0, 6)}
                      </div>
                      <div style={{ fontSize: '1.1rem', fontWeight: 800, marginTop: 4 }}>
                        {count}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            <h3 style={{ fontSize: '1rem', fontWeight: 700, marginTop: 10 }}>
              Cards Scheduled for {formatReadableDate(calendarDate)}:
            </h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {cards.filter(c => c.dueDate === calendarDate).map(c => (
                <div
                  key={c.id}
                  style={{
                    padding: 14,
                    backgroundColor: 'var(--bg-secondary)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--border-color)'
                  }}
                >
                  <p style={{ fontWeight: 600 }}>{c.question}</p>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: 4 }}>{c.answer}</p>
                </div>
              ))}
              {cards.filter(c => c.dueDate === calendarDate).length === 0 && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No cards scheduled for this date.</p>
              )}
            </div>
          </div>
        )}

        {/* Tab 4: LIBRARY */}
        {activeTab === 'library' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {/* Search and action bar */}
            <div style={{ display: 'flex', gap: 10 }}>
              <div style={{ position: 'relative', flex: 1 }}>
                <input
                  type="text"
                  placeholder="Search questions, answers or tags..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  style={{ width: '100%', paddingLeft: 38 }}
                />
                <Search size={16} style={{ position: 'absolute', left: 12, top: 12, color: 'var(--text-muted)' }} />
              </div>
              <button
                onClick={exportCSV}
                style={{
                  padding: '10px 14px',
                  backgroundColor: 'var(--bg-secondary)',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  gap: 6
                }}
              >
                <Download size={16} /> Export
              </button>
              <label style={{
                padding: '10px 14px',
                backgroundColor: 'var(--bg-secondary)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.85rem',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6
              }}>
                <Upload size={16} /> Import
                <input type="file" accept=".csv" onChange={handleImportFile} style={{ display: 'none' }} />
              </label>
            </div>

            {/* List */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {cards
                .filter(c => {
                  const matchFolder = c.folderId === activeFolderId;
                  const query = searchQuery.toLowerCase();
                  const matchQuery = !searchQuery ||
                    c.question.toLowerCase().includes(query) ||
                    c.answer.toLowerCase().includes(query) ||
                    c.tags.toLowerCase().includes(query);
                  return matchFolder && matchQuery;
                })
                .map(card => (
                  <div
                    key={card.id}
                    style={{
                      padding: 16,
                      backgroundColor: 'var(--bg-card)',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-color)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'flex-start',
                      gap: 12
                    }}
                  >
                    <div style={{ flex: 1 }}>
                      <p style={{ fontWeight: 700, fontSize: '0.95rem' }}>{card.question}</p>
                      <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: 4 }}>{card.answer}</p>
                      <div style={{ display: 'flex', gap: 8, marginTop: 8, fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        <span>Due: {formatReadableDate(card.dueDate)}</span>
                        <span>Level: {card.reviewStep + 1}</span>
                        {card.tags && <span style={{ color: 'var(--accent-primary)' }}>#{card.tags}</span>}
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button
                        onClick={() => openCardModal(card)}
                        style={{
                          padding: 6,
                          color: 'var(--text-secondary)',
                          backgroundColor: 'var(--bg-secondary)',
                          borderRadius: 'var(--radius-sm)'
                        }}
                      >
                        <Edit2 size={16} />
                      </button>
                      <button
                        onClick={() => deleteCard(card.id)}
                        style={{
                          padding: 6,
                          color: 'var(--danger)',
                          backgroundColor: 'var(--danger-bg)',
                          borderRadius: 'var(--radius-sm)'
                        }}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </div>
                ))}
            </div>
          </div>
        )}

        {/* Tab 5: STATS */}
        {activeTab === 'stats' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <h2 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Study Stats & PWA Info</h2>
            <div style={{
              backgroundColor: 'var(--bg-card)',
              padding: 20,
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border-color)',
              display: 'flex',
              flexDirection: 'column',
              gap: 14
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Total Cards in App:</span>
                <span style={{ fontWeight: 700 }}>{cards.length}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Folders:</span>
                <span style={{ fontWeight: 700 }}>{folders.length}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Cards Due Today:</span>
                <span style={{ fontWeight: 700, color: 'var(--danger)' }}>{cards.filter(c => c.dueDate <= todayStr).length}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Offline PWA Capability:</span>
                <span style={{ fontWeight: 700, color: 'var(--success)' }}>Active (Service Worker enabled)</span>
              </div>
            </div>

            <div style={{
              backgroundColor: 'var(--bg-secondary)',
              padding: 16,
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-color)',
              fontSize: '0.85rem',
              color: 'var(--text-secondary)',
              lineHeight: 1.5
            }}>
              <strong style={{ color: 'var(--text-primary)' }}>📱 How to Install as an App:</strong>
              <ul style={{ marginTop: 8, paddingLeft: 18 }}>
                <li><strong>Chrome / Android:</strong> Tap the 3 dots menu → tap <em>"Install App"</em> or <em>"Add to Home Screen"</em>.</li>
                <li><strong>Safari / iPhone:</strong> Tap the Share button → tap <em>"Add to Home Screen"</em>.</li>
                <li><strong>Desktop:</strong> Click the install icon in the URL address bar.</li>
              </ul>
            </div>
          </div>
        )}
      </main>

      {/* Bottom Navigation Bar */}
      <nav style={{
        position: 'sticky',
        bottom: 0,
        backgroundColor: 'var(--bg-secondary)',
        borderTop: '1px solid var(--border-color)',
        display: 'flex',
        justifyContent: 'space-around',
        padding: '10px 0',
        zIndex: 50
      }}>
        {[
          { id: 'study', label: 'Study', icon: BookOpen },
          { id: 'quiz', label: 'Quiz', icon: HelpCircle },
          { id: 'calendar', label: 'Schedule', icon: CalendarIcon },
          { id: 'library', label: 'Cards', icon: Layers },
          { id: 'stats', label: 'Stats', icon: BarChart3 },
        ].map(item => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => {
                setActiveTab(item.id as Tab);
                if (item.id === 'quiz') startQuiz();
              }}
              style={{
                flexDirection: 'column',
                gap: 4,
                fontSize: '0.75rem',
                fontWeight: 600,
                color: isActive ? 'var(--accent-primary)' : 'var(--text-muted)'
              }}
            >
              <Icon size={20} />
              {item.label}
            </button>
          );
        })}
      </nav>

      {/* Add / Edit Card Modal */}
      {showAddCard && (
        <div style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.65)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 20,
          zIndex: 200
        }}>
          <div style={{
            backgroundColor: 'var(--bg-card)',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            padding: 24,
            width: '100%',
            maxWidth: 480,
            display: 'flex',
            flexDirection: 'column',
            gap: 16
          }}>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 800 }}>
              {editingCard ? 'Edit Flashcard' : 'Create New Flashcard'}
            </h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Question</label>
              <textarea
                rows={3}
                placeholder="Front of the card (question or prompt)"
                value={formQuestion}
                onChange={(e) => setFormQuestion(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Answer</label>
              <textarea
                rows={3}
                placeholder="Back of the card (answer or explanation)"
                value={formAnswer}
                onChange={(e) => setFormAnswer(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Tags (comma-separated)</label>
              <input
                type="text"
                placeholder="e.g. biology, exam1"
                value={formTags}
                onChange={(e) => setFormTags(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
              <button
                onClick={closeCardModal}
                style={{
                  padding: '10px 16px',
                  backgroundColor: 'var(--bg-secondary)',
                  borderRadius: 'var(--radius-sm)',
                  fontWeight: 600
                }}
              >
                Cancel
              </button>
              <button
                onClick={saveCard}
                style={{
                  padding: '10px 18px',
                  backgroundColor: 'var(--accent-primary)',
                  color: '#ffffff',
                  borderRadius: 'var(--radius-sm)',
                  fontWeight: 600
                }}
              >
                Save Card
              </button>
            </div>
          </div>
        </div>
      )}

      {/* New Folder Modal */}
      {showAddFolder && (
        <div style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.65)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 20,
          zIndex: 200
        }}>
          <div style={{
            backgroundColor: 'var(--bg-card)',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--border-color)',
            padding: 24,
            width: '100%',
            maxWidth: 400,
            display: 'flex',
            flexDirection: 'column',
            gap: 16
          }}>
            <h2 style={{ fontSize: '1.2rem', fontWeight: 800 }}>Create New Folder</h2>
            <input
              type="text"
              placeholder="Folder name (e.g. Spanish, Anatomy)"
              value={newFolderName}
              onChange={(e) => setNewFolderName(e.target.value)}
              autoFocus
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button
                onClick={() => setShowAddFolder(false)}
                style={{
                  padding: '10px 16px',
                  backgroundColor: 'var(--bg-secondary)',
                  borderRadius: 'var(--radius-sm)',
                  fontWeight: 600
                }}
              >
                Cancel
              </button>
              <button
                onClick={addFolder}
                style={{
                  padding: '10px 18px',
                  backgroundColor: 'var(--accent-primary)',
                  color: '#ffffff',
                  borderRadius: 'var(--radius-sm)',
                  fontWeight: 600
                }}
              >
                Create
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function varToString(varName: string) {
  return `var(${varName})`;
}
