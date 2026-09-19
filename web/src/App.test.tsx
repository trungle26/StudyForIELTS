import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import App from './App';

vi.mock('./api', () => ({ getLessons: vi.fn().mockResolvedValue({ items: [{ id: '1', title: 'Practice', level: 'B1', audioUrl: 'audio.mp3', vocabularies: [] }] }), getLesson: vi.fn() }));
describe('lesson rendering', () => { it('renders a remote lesson', async () => { render(<App />); expect(await screen.findByText('Practice')).toBeTruthy(); }); });
