from __future__ import annotations

import html
import re
from typing import Any


CLASSIFIER_VERSION = "local-readability-v1"
TIMESTAMP_AWARE_VERSION = "local-readability-speed-v1"
LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"]

# Speed is reported separately from linguistic difficulty. Thresholds are
# conservative defaults; tweak only after measuring real transcripts.
SPEED_LABELS = ["slow", "normal", "fast", "very_fast"]
SPEED_WPM_THRESHOLDS = (90, 150, 170)  # upper bounds for slow, normal, fast.

CEFR_VOCABULARY = {
    "A1": [
        "a", "about", "and", "animal", "answer", "apple", "ask", "bad", "be", "beautiful",
        "because", "big", "book", "boy", "buy", "can", "car", "child", "city", "class",
        "come", "day", "do", "eat", "family", "friend", "get", "girl", "go", "good",
        "happy", "have", "home", "house", "i", "know", "like", "listen", "live", "make",
        "man", "many", "money", "mother", "name", "new", "not", "now", "people", "play",
        "read", "school", "see", "small", "speak", "student", "teacher", "time", "want",
        "water", "way", "week", "woman", "work", "write", "you",
    ],
    "A2": [
        "accident", "airport", "alone", "already", "arrive", "article", "believe", "business",
        "careful", "cheap", "choose", "college", "comfortable", "conversation", "customer",
        "different", "difficult", "direction", "doctor", "early", "enough", "environment",
        "explain", "favorite", "foreign", "future", "holiday", "important", "information",
        "internet", "journey", "language", "library", "machine", "market", "meeting", "message",
        "minute", "modern", "museum", "necessary", "office", "opinion", "possible", "problem",
        "quiet", "reason", "remember", "restaurant", "return", "simple", "special", "station",
        "suggest", "ticket", "travel", "usually", "weather", "without", "yesterday",
    ],
    "B1": [
        "achieve", "advantage", "although", "application", "arrangement", "available", "behavior",
        "challenge", "compare", "concern", "condition", "confirm", "consider", "continue",
        "culture", "decision", "degree", "develop", "difference", "education", "effective",
        "encourage", "experience", "factor", "government", "however", "improve", "include",
        "individual", "industry", "influence", "instead", "international", "introduce",
        "knowledge", "manager", "method", "opportunity", "organization", "percent", "personal",
        "population", "prepare", "probably", "provide", "quality", "relationship", "require",
        "research", "resource", "responsible", "result", "similar", "situation", "society",
        "support", "therefore", "training", "understand", "various",
    ],
    "B2": [
        "accurate", "alternative", "approach", "assessment", "assumption", "authority", "benefit",
        "capacity", "circumstance", "complex", "conclusion", "consequence", "consistent", "context",
        "contrast", "criteria", "debate", "demonstrate", "despite", "economic", "emphasis",
        "evidence", "financial", "flexible", "function", "identify", "impact", "interpret",
        "issue", "maintain", "majority", "network", "perspective", "potential", "previous",
        "principle", "priority", "process", "professional", "region", "regulation", "relevant",
        "reliable", "significant", "specific", "strategy", "structure", "sufficient", "technical",
        "widespread",
    ],
    "C1": [
        "abstract", "acknowledge", "advocate", "ambiguous", "analytical", "anticipate", "attribute",
        "comprehensive", "controversial", "criterion", "deduce", "differentiate", "dimension",
        "discourse", "emerge", "empirical", "equivalent", "establish", "evaluate", "explicit",
        "framework", "implicit", "inherent", "innovation", "insight", "integrate", "justify",
        "legislation", "mechanism", "nevertheless", "notion", "objective", "phenomenon", "precise",
        "predominantly", "prospect", "qualitative", "reinforce", "scope", "subsequent", "sustain",
        "theoretical", "transition", "underlying", "valid", "whereas",
    ],
    "C2": [
        "aberration", "acquiesce", "alleviate", "anomaly", "arbitrary", "catalyst", "coherent",
        "conjecture", "convoluted", "dichotomy", "elucidate", "exacerbate", "formidable",
        "inadvertent", "juxtapose", "meticulous", "nuanced", "paradigm", "pervasive", "plausible",
        "pragmatic", "profound", "reconcile", "scrutinize", "substantiate", "tenuous", "ubiquitous",
    ],
}

WORD_LEVEL = {word: level for level, words in CEFR_VOCABULARY.items() for word in words}
ADVANCED_SUFFIXES = ("tion", "sion", "ment", "ance", "ence", "ity", "ism", "ology", "ative", "istic", "ization")


def classify_cefr(transcript_text: str) -> dict[str, Any]:
    words = tokenize_words(transcript_text)
    sentences = split_sentences(transcript_text)

    if len(words) < 30:
        return {
            "level": "A1",
            "confidence": 0.2,
            "metrics": {
                "wordCount": len(words),
                "sentenceCount": len(sentences),
                "reason": "Transcript is too short for reliable classification.",
            },
            "explanation": "Short transcripts default to A1 with low confidence.",
            "classifierVersion": CLASSIFIER_VERSION,
        }

    unique_words = set(words)
    syllable_count = sum(count_syllables(word) for word in words)
    sentence_count = max(len(sentences), 1)
    word_count = len(words)
    avg_sentence_length = word_count / sentence_count
    avg_syllables_per_word = syllable_count / word_count
    flesch_kincaid_grade = max(0, 0.39 * avg_sentence_length + 11.8 * avg_syllables_per_word - 15.59)
    long_word_ratio = len([word for word in words if len(word) >= 8]) / word_count
    advanced_marker_ratio = len([word for word in words if is_advanced_marker(word)]) / word_count
    unique_word_ratio = len(unique_words) / word_count
    vocabulary_distribution = build_vocabulary_distribution(words)

    indicators = [
        grade_to_level_index(flesch_kincaid_grade),
        sentence_length_to_level_index(avg_sentence_length),
        lexical_to_level_index(long_word_ratio, advanced_marker_ratio, unique_word_ratio),
        vocabulary_to_level_index(vocabulary_distribution),
    ]
    weights = [0.35, 0.2, 0.3, 0.15]
    weighted_score = sum(indicator * weights[index] for index, indicator in enumerate(indicators))
    level_index = clamp(round(weighted_score), 0, len(LEVELS) - 1)
    level = LEVELS[level_index]

    return {
        "level": level,
        "confidence": confidence_from_agreement(indicators, word_count),
        "metrics": {
            "wordCount": word_count,
            "sentenceCount": sentence_count,
            "uniqueWordCount": len(unique_words),
            "avgSentenceLength": rounded(avg_sentence_length),
            "avgSyllablesPerWord": rounded(avg_syllables_per_word),
            "fleschKincaidGrade": rounded(flesch_kincaid_grade),
            "longWordRatio": rounded(long_word_ratio),
            "advancedMarkerRatio": rounded(advanced_marker_ratio),
            "uniqueWordRatio": rounded(unique_word_ratio),
            "vocabularyDistribution": vocabulary_distribution,
            "indicatorLevels": [LEVELS[indicator] for indicator in indicators],
            "weightedScore": rounded(weighted_score),
        },
        "explanation": (
            f"Estimated {level} from readability grade {rounded(flesch_kincaid_grade)}, "
            f"average sentence length {rounded(avg_sentence_length)}, "
            f"long-word ratio {rounded(long_word_ratio)}, and "
            f"advanced-marker ratio {rounded(advanced_marker_ratio)}."
        ),
        "classifierVersion": CLASSIFIER_VERSION,
    }


def count_words(text: str) -> int:
    return len(tokenize_words(text))


def tokenize_words(text: str) -> list[str]:
    return re.findall(r"[a-z]+(?:'[a-z]+)?", html.unescape(text).lower())


def split_sentences(text: str) -> list[str]:
    sentences = [sentence.strip() for sentence in re.split(r"[.!?]+", re.sub(r"\s+", " ", text)) if sentence.strip()]
    return sentences or ([text.strip()] if text.strip() else [])


def count_syllables(word: str) -> int:
    normalized = re.sub(r"(?:e|ed|es)$", "", word.lower())
    return max(len(re.findall(r"[aeiouy]+", normalized)), 1)


def is_advanced_marker(word: str) -> bool:
    return (
        len(word) >= 10
        or any(word.endswith(suffix) for suffix in ADVANCED_SUFFIXES)
        or word in {"however", "therefore", "whereas", "nevertheless", "consequently"}
    )


def build_vocabulary_distribution(words: list[str]) -> dict[str, Any]:
    distribution = {level: 0 for level in LEVELS}
    matched_words = 0
    for word in words:
        level = WORD_LEVEL.get(word)
        if not level:
            continue
        distribution[level] += 1
        matched_words += 1

    return {
        **distribution,
        "matchedWords": matched_words,
        "matchedRatio": rounded(matched_words / len(words)) if words else 0,
    }


def grade_to_level_index(grade: float) -> int:
    if grade <= 2:
        return 0
    if grade <= 4:
        return 1
    if grade <= 6:
        return 2
    if grade <= 8.5:
        return 3
    if grade <= 11:
        return 4
    return 5


def sentence_length_to_level_index(avg_sentence_length: float) -> int:
    if avg_sentence_length <= 8:
        return 0
    if avg_sentence_length <= 12:
        return 1
    if avg_sentence_length <= 16:
        return 2
    if avg_sentence_length <= 22:
        return 3
    if avg_sentence_length <= 28:
        return 4
    return 5


def lexical_to_level_index(long_word_ratio: float, advanced_marker_ratio: float, unique_word_ratio: float) -> int:
    score = long_word_ratio * 3.5 + advanced_marker_ratio * 8 + unique_word_ratio * 0.8
    if score <= 0.45:
        return 0
    if score <= 0.65:
        return 1
    if score <= 0.85:
        return 2
    if score <= 1.05:
        return 3
    if score <= 1.25:
        return 4
    return 5


def vocabulary_to_level_index(distribution: dict[str, Any]) -> int:
    if distribution["matchedWords"] < 10 or distribution["matchedRatio"] < 0.08:
        return 2
    weighted = sum(distribution[level] * index for index, level in enumerate(LEVELS))
    return clamp(round(weighted / distribution["matchedWords"]), 0, len(LEVELS) - 1)


def confidence_from_agreement(indicators: list[int], word_count: int) -> float:
    mean = sum(indicators) / len(indicators)
    variance = sum((indicator - mean) ** 2 for indicator in indicators) / len(indicators)
    agreement = max(0, 1 - variance**0.5 / 2.5)
    length_factor = min(1, word_count / 350)
    return rounded(0.25 + agreement * 0.5 + length_factor * 0.25)


def clamp(value: int, minimum: int, maximum: int) -> int:
    return min(max(value, minimum), maximum)


def rounded(value: float) -> float:
    return round(value, 3)


# ---------------------------------------------------------------------------
# Timestamp-aware classification (dictation)
# ---------------------------------------------------------------------------
def _speech_span_seconds(segments: list[dict[str, Any]] | None) -> float:
    """Union of all valid [start, end] intervals, in seconds.

    Used to compute speech-span WPM so that long silent gaps don't inflate
    the difficulty signal. Overlapping/duplicate intervals are merged.
    """
    if not segments:
        return 0.0
    intervals: list[tuple[float, float]] = []
    for segment in segments:
        try:
            start = float(segment["start"])
            end = float(segment["end"])
        except (KeyError, TypeError, ValueError):
            continue
        if end > start:
            intervals.append((start, end))
    if not intervals:
        return 0.0
    intervals.sort()
    total = 0.0
    current_start, current_end = intervals[0]
    for start, end in intervals[1:]:
        if start <= current_end:
            current_end = max(current_end, end)
        else:
            total += current_end - current_start
            current_start, current_end = start, end
    total += current_end - current_start
    return total


def _wpm(word_count: int, seconds: float) -> float:
    if seconds <= 0 or word_count <= 0:
        return 0.0
    return rounded(word_count / seconds * 60)


def classify_speed(words_per_minute: float) -> str:
    if words_per_minute <= 0:
        return "normal"
    if words_per_minute < SPEED_WPM_THRESHOLDS[0]:
        return SPEED_LABELS[0]
    if words_per_minute < SPEED_WPM_THRESHOLDS[1]:
        return SPEED_LABELS[1]
    if words_per_minute < SPEED_WPM_THRESHOLDS[2]:
        return SPEED_LABELS[2]
    return SPEED_LABELS[3]


def classify_dictation_cefr(
    transcript_text: str,
    segments: list[dict[str, Any]] | None = None,
    duration_seconds: float | None = None,
) -> dict[str, Any]:
    """Combine transcript readability with timestamp-aware speed.

    The linguistic CEFR level is determined by the existing readability
    pipeline. Speed is reported separately as ``speedDifficulty`` and may
    bump the returned level by at most one band when the content is
    already borderline, so a fast but easy recording never becomes C1
    solely because of WPM. Short or low-confidence results are marked
    ``reviewRecommended=True`` for manual confirmation.
    """
    base = classify_cefr(transcript_text)
    base_level_index = LEVELS.index(base["level"])

    words = tokenize_words(transcript_text)
    word_count = len(words)
    speech_seconds = _speech_span_seconds(segments)
    media_seconds = float(duration_seconds) if duration_seconds and duration_seconds > 0 else speech_seconds
    full_wpm = _wpm(word_count, media_seconds)
    speech_wpm = _wpm(word_count, speech_seconds)
    speed_difficulty = classify_speed(speech_wpm or full_wpm)

    # Speed may only nudge the level by one band and only when content is
    # borderline — never promotes A1 to C1 just because someone talks fast.
    adjusted_index = base_level_index
    if speed_difficulty in {"fast", "very_fast"} and base["confidence"] >= 0.5:
        adjusted_index = min(base_level_index + 1, len(LEVELS) - 1)
    elif speed_difficulty == "slow" and base_level_index > 0 and base["confidence"] >= 0.7:
        adjusted_index = base_level_index - 1

    review_recommended = (
        word_count < 30
        or base["confidence"] < 0.65
        or speed_difficulty in {"fast", "very_fast"}
    )

    return {
        "level": LEVELS[adjusted_index],
        "confidence": base["confidence"],
        "speedDifficulty": speed_difficulty,
        "reviewRecommended": review_recommended,
        "metrics": {
            **base["metrics"],
            "mediaDurationSeconds": rounded(media_seconds),
            "speechDurationSeconds": rounded(speech_seconds),
            "wordsPerMinuteFull": full_wpm,
            "wordsPerMinuteSpeech": speech_wpm,
        },
        "explanation": (
            base["explanation"]
            + f" Delivery speed: {speed_difficulty} "
            f"(speech-span WPM {speech_wpm}, full WPM {full_wpm})."
        ),
        "classifierVersion": TIMESTAMP_AWARE_VERSION,
    }

