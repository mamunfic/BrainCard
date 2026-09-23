export interface Flashcard {
  id: string;
  folderId: string;
  question: string;
  answer: string;
  tags: string;
  reviewStep: number; // 0..4 (indices for intervals [1, 3, 7, 14, 30])
  dueDate: string; // YYYY-MM-DD
  lastReviewed: string | null;
  imageUri?: string | null;
  createdAt: number;
}

export interface Folder {
  id: string;
  name: string;
  createdAt: number;
}

export const REVIEW_INTERVALS = [1, 3, 7, 14, 30];

export function getTodayDateString(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function getDateStringAfterDays(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatReadableDate(dateString: string): string {
  if (!dateString) return '';
  try {
    const parts = dateString.split('-');
    if (parts.length === 3) {
      const date = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
      return date.toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' });
    }
    return dateString;
  } catch {
    return dateString;
  }
}
