import { Flashcard, Folder, getTodayDateString, getDateStringAfterDays } from './types';

const FOLDERS_KEY = 'braincard_folders';
const CARDS_KEY = 'braincard_cards';

export const initialFolders: Folder[] = [
  { id: 'general', name: 'General', createdAt: Date.now() },
  { id: 'science', name: 'Science & Tech', createdAt: Date.now() + 10 },
  { id: 'languages', name: 'Languages', createdAt: Date.now() + 20 }
];

export const initialCards: Flashcard[] = [
  {
    id: 'card_seed_1',
    folderId: 'general',
    question: 'What is the Spaced Repetition effect?',
    answer: 'A learning technique where reviews are spaced at increasing intervals (e.g. 1d, 3d, 7d, 14d, 30d) to optimize long-term retention and fight the forgetting curve.',
    tags: 'learning, psychology, braincard',
    reviewStep: 0,
    dueDate: getTodayDateString(),
    lastReviewed: null,
    createdAt: Date.now()
  },
  {
    id: 'card_seed_2',
    folderId: 'general',
    question: 'How does BrainCard interval scheduling work?',
    answer: "When you tap 'Remembered', the interval expands: 1 → 3 → 7 → 14 → 30 days.\nIf you tap 'Forgot', the card resets to 1 day for prompt reinforcement.",
    tags: 'spaced-repetition, study',
    reviewStep: 1,
    dueDate: getTodayDateString(),
    lastReviewed: null,
    createdAt: Date.now() + 1
  },
  {
    id: 'card_seed_3',
    folderId: 'science',
    question: 'What is the primary function of Mitochondria?',
    answer: "Mitochondria generate most of the cell's supply of adenosine triphosphate (ATP), used as a source of chemical energy (the 'powerhouse of the cell').",
    tags: 'biology, cells, science',
    reviewStep: 0,
    dueDate: getTodayDateString(),
    lastReviewed: null,
    createdAt: Date.now() + 2
  },
  {
    id: 'card_seed_4',
    folderId: 'science',
    question: 'What is a Progressive Web App (PWA)?',
    answer: 'A web app built with modern APIs to deliver native app-like capabilities, reliability, and installability with offline support across all devices.',
    tags: 'web, pwa, technology',
    reviewStep: 2,
    dueDate: getDateStringAfterDays(1),
    lastReviewed: null,
    createdAt: Date.now() + 3
  },
  {
    id: 'card_seed_5',
    folderId: 'languages',
    question: "How do you say 'Thank you very much' in Japanese?",
    answer: 'どうもありがとうございます (Dōmo arigatō gozaimasu)',
    tags: 'japanese, phrase, vocabulary',
    reviewStep: 1,
    dueDate: getDateStringAfterDays(3),
    lastReviewed: null,
    createdAt: Date.now() + 4
  }
];

export function loadStoredFolders(): Folder[] {
  try {
    const raw = localStorage.getItem(FOLDERS_KEY);
    if (!raw) {
      saveStoredFolders(initialFolders);
      return initialFolders;
    }
    return JSON.parse(raw);
  } catch {
    return initialFolders;
  }
}

export function saveStoredFolders(folders: Folder[]) {
  try {
    localStorage.setItem(FOLDERS_KEY, JSON.stringify(folders));
  } catch (e) {
    console.error('Failed to save folders', e);
  }
}

export function loadStoredCards(): Flashcard[] {
  try {
    const raw = localStorage.getItem(CARDS_KEY);
    if (!raw) {
      saveStoredCards(initialCards);
      return initialCards;
    }
    return JSON.parse(raw);
  } catch {
    return initialCards;
  }
}

export function saveStoredCards(cards: Flashcard[]) {
  try {
    localStorage.setItem(CARDS_KEY, JSON.stringify(cards));
  } catch (e) {
    console.error('Failed to save cards', e);
  }
}
