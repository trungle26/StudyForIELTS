import { describe, expect, it, vi } from 'vitest';
import { getLessons } from './api';

describe('dictation API mapping', () => {
  it('returns the server lesson items without changing the contract', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ items: [{ id: '1', title: 'Practice', level: 'B1', audioUrl: 'audio.mp3', vocabularies: [] }], total: 1 }) }));
    await expect(getLessons()).resolves.toMatchObject({ total: 1, items: [{ id: '1', audioUrl: 'audio.mp3' }] });
  });
});
