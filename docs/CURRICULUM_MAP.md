# Curriculum Map

## Level model

The curriculum uses CEFR for language difficulty and IELTS bands for exam goals. They are related but not interchangeable.

| Internal stage | Typical CEFR range | Main purpose |
|---|---|---|
| Beginner | A1 | Build survival English, pronunciation, and basic sentence control |
| Elementary | A2 | Build everyday grammar, topic vocabulary, and short-text comprehension |
| Pre-intermediate | B1 | Develop connected sentences, general listening, reading, and basic opinions |
| Intermediate | B1–B2 | Transition into IELTS skill practice and longer language tasks |
| Upper-intermediate | B2–C1 | Target IELTS strategies, accuracy, fluency, and exam timing |
| Advanced | C1–C2 | Refine precision, flexibility, task response, and high-band performance |

The mapping is a recommendation, not an IELTS score conversion.

## Foundation curriculum

### Beginner: A1

- Pronunciation: alphabet, common sounds, word stress, basic phonemic symbols.
- Grammar: be, have, pronouns, plurals, present simple, basic questions, articles.
- Vocabulary: personal information, family, home, food, numbers, time, daily routines.
- Listening: slow short sentences, repetition, transcript-supported dictation.
- Reading: words, signs, notices, short messages, simple sentences.
- Writing: copying, word ordering, sentence completion, personal information.
- Speaking: listen-and-repeat, introductions, basic question and answer.

### Elementary: A2

- Pronunciation: connected speech awareness and common pronunciation problems.
- Grammar: past and future forms, present continuous, comparatives, modals, prepositions.
- Vocabulary: health, transport, shopping, work, study, weather, common places.
- Listening: short conversations, key-word recognition, slower natural speech.
- Reading: short paragraphs, main idea, details, context clues.
- Writing: routines, descriptions, short messages, connected sentences.
- Speaking: preferences, routines, past experiences, reasons.

### Pre-intermediate: B1

- Grammar: present perfect, conditionals, relative clauses, passive voice, reported speech.
- Vocabulary: education, environment, technology, society, media, employment.
- Listening: natural conversations, reduced forms, partial transcript dictation.
- Reading: longer articles, skimming, scanning, reference words.
- Writing: topic sentences, supporting details, examples, paragraph organization.
- Speaking: descriptions, explanations, opinions, comparisons.

## IELTS transition curriculum

### Intermediate: B1–B2

- Introduce IELTS task formats gradually.
- Teach question instructions and common distractors.
- Use shorter passages and reduced time pressure first.
- Introduce Task 1 overview and comparison language.
- Introduce Task 2 thesis statements and body paragraphs.
- Begin speaking Part 1 and short Part 2 responses.

### Upper-intermediate: B2–C1

- Full IELTS listening and reading question types.
- Timed sections and complete attempts.
- Task 1 chart selection, overview, comparison, and data accuracy.
- Task 2 planning, coherence, examples, and position development.
- Speaking Parts 1–3 with fluency and lexical resource feedback.
- Target-band strategy lessons.

### Advanced: C1–C2

- Precision and register.
- Complex but controlled sentence structures.
- Paraphrasing and nuanced vocabulary.
- Difficult accents, lectures, and abstract reading.
- High-band writing revision.
- Independent error analysis and exam simulations.

## Content domains

Every lesson should be tagged with:

```text
stage
cefrLevel
skill
topic
contentType
prerequisites
estimatedMinutes
```

Possible `contentType` values:

- grammar_lesson
- grammar_exercise
- pronunciation_lesson
- vocabulary_lesson
- vocabulary_review
- listening_lesson
- dictation
- reading_lesson
- reading_question_set
- speaking_drill
- speaking_prompt
- writing_exercise
- writing_task1
- writing_task2
- strategy_lesson

## Progression gates

A learner should move forward based on evidence, not only elapsed time:

- Complete the lesson.
- Reach the configured exercise threshold.
- Review missed items.
- Retry when the lesson requires mastery.
- Revisit weak skills through recommendations.

Do not block the learner permanently. Allow exploration while making the recommended path clear.

## Recommended first content slice

The first cross-feature curriculum slice should be one beginner-friendly topic:

```text
Topic: Daily routines
  -> Topic vocabulary
  -> Present simple grammar
  -> Short listening dictation
  -> Sentence writing
  -> Vocabulary review
```

This slice is small, measurable, and can reuse the existing Room progress and dictation infrastructure.
