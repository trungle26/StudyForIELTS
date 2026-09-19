import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { getLesson, getLessons } from './api';

vi.mock('./api', async () => ({ ...(await vi.importActual<typeof import('./api')>('./api')), getLessons: vi.fn(), getLesson: vi.fn() }));
const lesson = { id: '1', title: 'Daily routines', level: 'A1', audioUrl: 'audio.mp3', vocabularies: [{ word: 'wake up', phonetic: '', meaning: 'get out of bed', exampleSentence: 'I wake up early.' }], sentences: [] };
beforeEach(() => { location.hash = ''; localStorage.clear(); vi.mocked(getLessons).mockResolvedValue({ items: [lesson] } as never); vi.mocked(getLesson).mockResolvedValue({ lesson } as never); });
describe('lesson rendering', () => {
  it('renders a remote lesson', async () => { render(<App />); expect(await screen.findByText('Daily routines')).toBeTruthy(); });
  it('gives feedback, retries missed answers, and persists completion', async () => { render(<App />); fireEvent.click(await screen.findByRole('button', { name: /daily routines review/i })); fireEvent.change(await screen.findByLabelText('Your answer'), { target: { value: 'wrong' } }); fireEvent.click(screen.getByRole('button', { name: 'Check answer' })); expect(screen.getByRole('status').textContent).toContain('The answer is'); fireEvent.click(screen.getByRole('button', { name: 'Continue' })); expect(screen.getByText('Review complete')).toBeTruthy(); fireEvent.click(screen.getByRole('button', { name: 'Save completion' })); expect(localStorage.getItem('studyforielts.daily-routines.vocabulary')).toContain('completed'); });
});
