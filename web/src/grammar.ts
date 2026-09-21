export type GrammarExercise = { prompt: string; options: string[]; answer: string; explanation: string; explanationVi: string };
export type GrammarProgress = { index: number; missed: number[]; completed: boolean };

export const grammarExercises: GrammarExercise[] = [
  { prompt: 'I ___ up every day.', options: ['wake', 'wakes'], answer: 'wake', explanation: 'Use the base verb after I.', explanationVi: 'Sau I, dùng động từ nguyên mẫu.' },
  { prompt: 'She ___ breakfast in the morning.', options: ['eat', 'eats'], answer: 'eats', explanation: 'Use verb-s after she.', explanationVi: 'Sau she, thêm -s vào động từ.' },
  { prompt: 'We ___ to school every day.', options: ['go', 'goes'], answer: 'go', explanation: 'Use the base verb after we.', explanationVi: 'Sau we, dùng động từ nguyên mẫu.' },
  { prompt: 'He ___ at night.', options: ['sleep', 'sleeps'], answer: 'sleeps', explanation: 'Use verb-s after he.', explanationVi: 'Sau he, thêm -s vào động từ.' },
  { prompt: 'They study ___.', options: ['at night', 'every day'], answer: 'every day', explanation: 'Every day tells us how often they study.', explanationVi: 'Every day cho biết tần suất học.' },
];

export const emptyGrammarProgress: GrammarProgress = { index: 0, missed: [], completed: false };
export const isCorrectGrammarAnswer = (exercise: GrammarExercise, answer: string) => exercise.answer.toLocaleLowerCase() === answer.trim().toLocaleLowerCase();
export const readGrammarProgress = (storage: Storage | undefined): GrammarProgress => {
  try {
    const value = JSON.parse(storage?.getItem('studyforielts.daily-routines.grammar') ?? 'null');
    return value && Number.isInteger(value.index) && Array.isArray(value.missed) && typeof value.completed === 'boolean' ? value : emptyGrammarProgress;
  } catch { return emptyGrammarProgress; }
};
export const saveGrammarProgress = (storage: Storage | undefined, progress: GrammarProgress) => {
  try { storage?.setItem('studyforielts.daily-routines.grammar', JSON.stringify(progress)); } catch { /* storage is optional */ }
};
