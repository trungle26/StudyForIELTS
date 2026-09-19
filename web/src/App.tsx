import { useEffect, useState } from 'react';
import { getLesson, getLessons, Lesson } from './api';
import './styles.css';

export default function App() {
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [selected, setSelected] = useState<Lesson | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const load = () => { setLoading(true); setError(''); getLessons().then(data => setLessons(data.items)).catch(e => setError(e.message)).finally(() => setLoading(false)); };
  useEffect(load, []);
  const open = (id: string) => { setLoading(true); setError(''); getLesson(id).then(data => setSelected(data.lesson)).catch(e => setError(e.message)).finally(() => setLoading(false)); };
  return <main><header><p className="eyebrow">StudyForIELTS</p><h1>Dictation practice</h1><p>Listen closely, build vocabulary, and improve your IELTS comprehension.</p></header>
    {error && <section className="notice error" role="alert"><strong>Could not load lessons.</strong><span>{error}</span><button onClick={selected ? () => open(selected.id) : load}>Retry</button></section>}
    {loading && <p className="notice">Loading lessons…</p>}
    {!loading && !error && !selected && (lessons.length ? <section className="grid">{lessons.map(lesson => <button className="card" key={lesson.id} onClick={() => open(lesson.id)}><span className="badge">{lesson.level}</span><h2>{lesson.title}</h2><span>{lesson.durationSeconds ? `${lesson.durationSeconds}s` : 'Audio lesson'} · Open lesson →</span></button>)}</section> : <p className="notice">No published dictation lessons are available yet.</p>)}
    {!loading && selected && <section className="detail"><button className="back" onClick={() => setSelected(null)}>← All lessons</button><span className="badge">{selected.level}</span><h2>{selected.title}</h2><audio controls preload="metadata" src={selected.audioUrl}>Your browser does not support audio playback.</audio><h3>Vocabulary</h3>{selected.vocabularies.length ? <ul>{selected.vocabularies.map(item => <li key={item.word}><strong>{item.word}</strong> {item.phonetic && <em>/{item.phonetic}/</em>}<br />{item.meaning}{item.exampleSentence && <small>{item.exampleSentence}</small>}</li>)}</ul> : <p>No vocabulary has been added to this lesson.</p>}</section>}
  </main>;
}
