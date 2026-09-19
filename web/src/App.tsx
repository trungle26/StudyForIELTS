import { useEffect, useMemo, useState } from 'react';
import { getLesson, getLessons, Lesson, mapVocabulary, Vocabulary } from './api';
import './styles.css';

const lessonIdFromHash = () => decodeURIComponent(location.hash.replace(/^#\/lesson\//, ''));
const storageKey = 'studyforielts.daily-routines.vocabulary';
type Progress = { learned: string[]; missed: string[]; completed: boolean };
const emptyProgress: Progress = { learned: [], missed: [], completed: false };
const readProgress = (): Progress => { try { const value = JSON.parse(localStorage.getItem(storageKey) ?? 'null'); return value && Array.isArray(value.learned) && Array.isArray(value.missed) ? { ...emptyProgress, ...value } : emptyProgress; } catch { return emptyProgress; } };
const saveProgress = (progress: Progress) => { try { localStorage.setItem(storageKey, JSON.stringify(progress)); } catch { /* unavailable storage is non-fatal */ } };
const normalize = (value: string) => value.trim().toLocaleLowerCase().replace(/[.!?]+$/, '');

function VocabularyReview({ items, onBack }: { items: Vocabulary[]; onBack: () => void }) {
  const vocabulary = useMemo(() => mapVocabulary(items), [items]);
  const [progress, setProgress] = useState(readProgress);
  const [index, setIndex] = useState(0);
  const [answer, setAnswer] = useState('');
  const [feedback, setFeedback] = useState('');
  const current = vocabulary[index];
  const update = (next: Progress) => { setProgress(next); saveProgress(next); };
  if (!vocabulary.length) return <section className="detail"><button className="back" onClick={onBack}>← Back</button><p className="notice">No daily-routines vocabulary is available yet.</p></section>;
  if (progress.completed) return <section className="detail"><h2>Review complete</h2><p role="status">You completed daily-routines vocabulary review. Your progress is saved on this browser.</p><button onClick={() => update(emptyProgress)}>Review again</button></section>;
  if (!current) return <section className="detail"><h2>Review complete</h2><p role="status">Great work. You reviewed every item.</p><button onClick={() => update({ ...progress, completed: true })}>Save completion</button></section>;
  const submit = (value: string) => { setAnswer(value); const correct = normalize(value) === normalize(current.word); const next = correct ? progress : { ...progress, missed: [...new Set([...progress.missed, current.word])] }; setFeedback(correct ? `Correct. “${current.word}” means ${current.meaning}.` : `Not quite. The answer is “${current.word}” — ${current.meaning}.`); update(next); };
  const nextItem = () => { setFeedback(''); setAnswer(''); setIndex(index + 1); };
  return <section className="detail"><button className="back" onClick={onBack}>← Back</button><p className="eyebrow">Daily routines · A1 · Vocabulary</p><h2>Choose the word</h2><p>Which word matches: <strong>{current.meaning}</strong>?</p><label htmlFor="answer">Your answer</label><input id="answer" value={answer} onChange={event => setAnswer(event.target.value)} disabled={!!feedback} autoComplete="off" />{!feedback ? <button onClick={() => submit(answer)} disabled={!answer.trim()}>Check answer</button> : <p className={normalize(answer) === normalize(current.word) ? 'feedback correct' : 'feedback incorrect'} role="status">{feedback}</p>}{feedback && <button onClick={nextItem}>Continue</button>}<p>{index + 1} of {vocabulary.length}</p></section>;
}

export default function App() {
  const [lessons, setLessons] = useState<Lesson[]>([]); const [selected, setSelected] = useState<Lesson | null>(null); const [review, setReview] = useState(false); const [error, setError] = useState(''); const [loading, setLoading] = useState(true);
  const load = () => { setLoading(true); setError(''); getLessons().then(data => setLessons(data.items)).catch(e => setError(e.message)).finally(() => setLoading(false)); };
  useEffect(() => { const id = lessonIdFromHash(); if (id) open(id); else load(); const onHashChange = () => { const nextId = lessonIdFromHash(); nextId ? open(nextId) : setSelected(null); }; addEventListener('hashchange', onHashChange); return () => removeEventListener('hashchange', onHashChange); }, []);
  const open = (id: string) => { location.hash = `/lesson/${encodeURIComponent(id)}`; setLoading(true); setError(''); getLesson(id).then(data => setSelected(data.lesson)).catch(e => setError(e.message)).finally(() => setLoading(false)); };
  const close = () => { location.hash = ''; setSelected(null); setReview(false); };
  return <main><header><p className="eyebrow">StudyForIELTS</p><h1>Dictation practice</h1><p>Listen closely, build vocabulary, and improve your IELTS comprehension.</p></header>{error && <section className="notice error" role="alert"><strong>Could not load lesson.</strong><span>{error}</span><button onClick={selected ? () => open(selected.id) : load}>Retry</button></section>}{loading && <p className="notice" role="status">Loading lessons…</p>}{!loading && !error && !selected && (lessons.length ? <section className="grid">{lessons.map(lesson => <button className="card" key={lesson.id} onClick={() => open(lesson.id)}><span className="badge">{lesson.level}</span><h2>{lesson.title}</h2><span>{lesson.durationSeconds ? `${lesson.durationSeconds}s` : 'Audio lesson'} · Open lesson →</span></button>)}<button className="card" onClick={() => { setSelected(lessons[0]); setReview(true); }}><span className="badge">A1 · Vocabulary</span><h2>Daily routines review</h2><span>Review vocabulary →</span></button></section> : <p className="notice">No published dictation lessons are available yet.</p>)}{!loading && selected && (review ? <VocabularyReview items={selected.vocabularies} onBack={() => setReview(false)} /> : <section className="detail"><button className="back" onClick={close}>← All lessons</button><span className="badge">{selected.level}</span><h2>{selected.title}</h2><button onClick={() => setReview(true)}>Start vocabulary review</button><audio controls preload="metadata" src={selected.audioUrl}>Your browser does not support audio playback.</audio><h3>Sentences</h3>{selected.sentences?.length ? <ol>{selected.sentences.map(sentence => <li key={sentence.orderIndex}>{sentence.text}</li>)}</ol> : <p>No sentences have been added to this lesson.</p>}<h3>Vocabulary</h3>{selected.vocabularies.length ? <ul>{selected.vocabularies.map(item => <li key={item.word}><strong>{item.word}</strong> {item.phonetic && <em>/{item.phonetic}/</em>}<br />{item.meaning}{item.exampleSentence && <small>{item.exampleSentence}</small>}</li>)}</ul> : <p>No vocabulary has been added to this lesson.</p>}</section>)}</main>;
}
