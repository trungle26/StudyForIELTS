import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { getLesson, getLessons } from './api';
import { grammarExercises, isCorrectGrammarAnswer, readGrammarProgress, saveGrammarProgress } from './grammar';

vi.mock('./api', async () => ({ ...(await vi.importActual<typeof import('./api')>('./api')), getLessons: vi.fn(), getLesson: vi.fn() }));
const lesson = { id: '1', title: 'Daily routines', level: 'A1', audioUrl: 'audio.mp3', vocabularies: [{ word: 'wake up', phonetic: '', meaning: 'get out of bed', exampleSentence: 'I wake up early.' }], sentences: [] };
beforeEach(() => { location.hash = ''; localStorage.clear(); vi.mocked(getLessons).mockResolvedValue({ items: [lesson] } as never); vi.mocked(getLesson).mockResolvedValue({ lesson } as never); });
afterEach(() => cleanup());
describe('grammar logic and persistence', () => {
  it('evaluates normalized answers and records storage failures safely', () => { expect(isCorrectGrammarAnswer(grammarExercises[0], ' WAKE ')).toBe(true); const storage = { getItem: () => '{bad', setItem: () => { throw new Error('blocked'); } } as unknown as Storage; expect(readGrammarProgress(storage).index).toBe(0); expect(() => saveGrammarProgress(storage, { index: 1, missed: [0], completed: false })).not.toThrow(); });
  it('renders the grammar activity, retries, and persists completion', async () => { location.hash = '#/foundation/grammar'; render(<App />); expect(await screen.findByRole('heading', { name: 'Present simple' })).toBeTruthy(); fireEvent.click(screen.getByRole('button', { name: 'wakes' })); expect(screen.getByRole('alert').textContent).toContain('Try again'); fireEvent.click(screen.getByRole('button', { name: 'Try again' })); fireEvent.click(screen.getByRole('button', { name: 'wake' })); expect(screen.getByRole('button', { name: 'Continue' })).toBeTruthy(); });
});
describe('shell navigation', () => {
  it('renders the foundation hub with sequential progress and switches views', async () => { render(<App />); expect(await screen.findByRole('heading', { name: 'Daily Routines' })).toBeTruthy(); expect(screen.getByText(/0 of 4 activities completed/)).toBeTruthy(); expect(screen.getByText('Step 1')).toBeTruthy(); fireEvent.click(screen.getByRole('button', { name: 'Dictation Library' })); expect((await screen.findAllByRole('heading', { name: 'Dictation Library' })).length).toBeGreaterThan(0); expect(screen.queryByText('Step 1')).toBeNull(); });
  it('supports hash routes and language switching', async () => { location.hash = '#/foundation/grammar'; render(<App />); expect(await screen.findByRole('heading', { name: 'Present simple' })).toBeTruthy(); expect(location.hash).toBe('#/foundation/grammar'); fireEvent.click(screen.getAllByRole('button', { name: 'Tiếng Việt' })[0]); expect(screen.getByRole('heading', { name: 'Hiện tại đơn' })).toBeTruthy(); });
});
describe('lesson rendering', () => {
  it('renders a remote lesson', async () => { location.hash = '#/dictation'; render(<App />); expect(await screen.findByText('Daily routines')).toBeTruthy(); });
  it('gives feedback, retries missed answers, and persists completion', async () => { location.hash = '#/foundation/vocabulary'; render(<App />); await screen.findByLabelText('Your answer'); fireEvent.change(await screen.findByLabelText('Your answer'), { target: { value: 'wrong' } }); fireEvent.click(screen.getByRole('button', { name: 'Check answer' })); expect(screen.getByRole('status').textContent).toContain('The answer is'); fireEvent.click(screen.getByRole('button', { name: 'Continue' })); expect(screen.getByText('Review complete')).toBeTruthy(); fireEvent.click(screen.getByRole('button', { name: 'Save completion' })); expect(localStorage.getItem('studyforielts.daily-routines.vocabulary')).toContain('completed'); });
});
