export type Vocabulary = { word: string; phonetic: string; meaning: string; exampleSentence: string; translation?: string };
export type Sentence = { orderIndex: number; text: string; startTimeMs?: number; endTimeMs?: number };
export type Lesson = { id: string; title: string; level: string; audioUrl: string; durationSeconds?: number; tags?: string[]; sentences: Sentence[]; vocabularies: Vocabulary[] };
type ListResponse = { items: Lesson[]; total: number; page: number; limit: number; totalPages: number };

export const mapVocabulary = (items: Vocabulary[] = []) => items.filter(item => item.word.trim() && item.meaning.trim()).slice(0, 8);

const baseUrl = import.meta.env.VITE_BFF_URL ?? '';
async function request<T>(path: string): Promise<T> {
  try {
    const response = await fetch(`${baseUrl}${path}`);
    if (!response.ok) throw new Error(`Request failed (${response.status})`);
    return await response.json() as T;
  } catch (error) {
    if (!navigator.onLine) throw new Error('You appear to be offline. Check your connection and retry.');
    throw error;
  }
}
export const getLessons = () => request<ListResponse>('/dictation/lessons');
export const getLesson = (id: string) => request<{ lesson: Lesson }>(`/dictation/lessons/${encodeURIComponent(id)}`);
