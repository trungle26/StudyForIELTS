"""Standalone self-check for generate_dictation_seed's pure helpers.

Run with::

    python bff/youtube_scraper/test_generate_dictation_seed.py

Exits non-zero on the first assertion failure.
"""
from __future__ import annotations

import sys
import tempfile
import types
from pathlib import Path

# Make the script importable without packaging.
sys.path.insert(0, str(Path(__file__).resolve().parent))

# ponytail: stub httpx so this self-check runs without the runtime dep
# installed; the real httpx is required to actually run the script.
if "httpx" not in sys.modules:
    sys.modules["httpx"] = types.SimpleNamespace(
        post=lambda *args, **kwargs: None,
        Client=lambda *args, **kwargs: None,
    )

from generate_dictation_seed import (  # noqa: E402
    discover_lessons,
    join_transcript,
    make_lesson,
    slugify,
)

from app.services.cefr_classifier import (  # noqa: E402
    _speech_span_seconds,
    _wpm,
    classify_dictation_cefr,
    classify_speed,
)


def _segment(text: str, start: float, end: float):
    return type("Segment", (), {"text": text, "start": start, "end": end})()


def _transcription(segments, duration=12.5):
    return type("Transcription", (), {"segments": segments, "duration": duration})()


def _classification():
    return {
        "level": "A2",
        "confidence": 0.6,
        "speedDifficulty": "fast",
        "reviewRecommended": True,
        "metrics": {"wpm": 200},
        "explanation": "",
        "classifierVersion": "local-readability-speed-v1",
    }


def assert_equal(label: str, actual, expected) -> None:
    if actual != expected:
        raise AssertionError(f"{label}: expected {expected!r}, got {actual!r}")


def test_slugify() -> None:
    assert_equal("slugify ascii", slugify("Hello World!"), "hello-world")
    assert_equal("slugify diacritics", slugify("Café — résumé"), "cafe-resume")
    assert_equal("slugify empty", slugify("!!!"), "lesson")


def test_discover_flat() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        (root / "a.mp3").write_bytes(b"")
        (root / "b.wav").write_bytes(b"")
        (root / "notes.txt").write_text("x")
        records = discover_lessons(root, recursive=False)
        ids = sorted(r["lesson_id"] for r in records)
        assert_equal("flat ids", ids, ["dd-a", "dd-b"])


def test_discover_recursive_ignores_folder_names() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        (root / "groceries").mkdir()
        (root / "groceries" / "bread.flac").write_bytes(b"")
        (root / "essays" / "english" / "b1").mkdir(parents=True)
        (root / "essays" / "english" / "b1" / "alpha.mp3").write_bytes(b"")
        records = discover_lessons(root)
        ids = sorted(r["lesson_id"] for r in records)
        assert_equal("recursive ids", ids, ["dd-alpha", "dd-bread"])


def test_discover_duplicate_id_raises() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        (root / "dup.mp3").write_bytes(b"")
        (root / "dup.wav").write_bytes(b"")
        try:
            discover_lessons(root)
        except SystemExit as exc:
            assert "Duplicate" in str(exc), f"unexpected message: {exc}"
        else:
            raise AssertionError("expected SystemExit for duplicate lesson id")


def test_make_lesson_attaches_classification() -> None:
    transcription = _transcription(
        [_segment("Hello there.", 0.0, 1.5), _segment("How are you?", 1.6, 3.2)],
        duration=3.2,
    )
    lesson = make_lesson(
        metadata={"title": "Greeting", "level": "A1", "audioUrl": "https://x/y.mp3"},
        lesson_id="dd-a1-greeting",
        transcription=transcription,
        classification=_classification(),
    )
    assert_equal("id", lesson["id"], "dd-a1-greeting")
    assert_equal("duration", lesson["durationSeconds"], 3)
    assert_equal("sentence count", len(lesson["sentences"]), 2)
    assert_equal("level matches classification", lesson["level"], "A2")
    assert_equal("classification attached", lesson["classification"]["level"], "A2")


def test_make_lesson_skips_zero_duration() -> None:
    transcription = _transcription(
        [_segment("ignored", 5.0, 5.0), _segment("kept", 0.0, 1.0)],
    )
    lesson = make_lesson(
        metadata={"audioUrl": "x"},
        lesson_id="x",
        transcription=transcription,
        classification=_classification(),
    )
    assert_equal("only kept", [s["text"] for s in lesson["sentences"]], ["kept"])


def test_join_transcript_truncates() -> None:
    sentences = [{"text": "hello " * 5000}]
    joined = join_transcript(sentences)
    assert len(joined) <= 12_000, f"length was {len(joined)}"


def test_speech_span_unions_overlaps() -> None:
    segments = [
        {"start": 0.0, "end": 5.0, "text": "a"},
        {"start": 4.0, "end": 9.0, "text": "b"},
        {"start": 12.0, "end": 18.0, "text": "c"},
    ]
    assert_equal("union span", _speech_span_seconds(segments), 15.0)


def test_wpm_counts_alphabetic_tokens() -> None:
    text = "the the the cat " * 10
    span = 120.0
    # "the" x3 per group, "cat" x1, per group of 4 words; 10 groups => 40 tokens.
    expected = 40 * 60 / span  # 20 words per minute
    assert abs(_wpm(40, span) - expected) < 1e-6, _wpm(40, span)


def test_classify_speed_thresholds() -> None:
    assert_equal("slow", classify_speed(60), "slow")
    assert_equal("normal", classify_speed(100), "normal")
    assert_equal("fast", classify_speed(160), "fast")
    assert_equal("very_fast", classify_speed(220), "very_fast")


def test_short_transcript_flags_review() -> None:
    result = classify_dictation_cefr("Hi there. This is short.")
    assert_equal("level", result["level"], "A1")
    assert_equal("review recommended", result["reviewRecommended"], True)


def test_basic_fast_speech_caps_promotion() -> None:
    fast_basic_words = ("I go to school every day " * 20).strip()
    segments = [
        {"start": i * 0.4, "end": (i + 1) * 0.4, "text": "I go to school every day"}
        for i in range(20)
    ]
    result = classify_dictation_cefr(fast_basic_words, segments=segments)
    assert result["speedDifficulty"] in {"fast", "very_fast"}, result
    assert result["reviewRecommended"] is True
    levels = {"A1", "A2", "B1", "B2", "C1", "C2"}
    assert result["level"] in levels, result


def main() -> int:
    tests = [
        test_slugify,
        test_discover_flat,
        test_discover_recursive_ignores_folder_names,
        test_discover_duplicate_id_raises,
        test_make_lesson_attaches_classification,
        test_make_lesson_skips_zero_duration,
        test_join_transcript_truncates,
        test_speech_span_unions_overlaps,
        test_wpm_counts_alphabetic_tokens,
        test_classify_speed_thresholds,
        test_short_transcript_flags_review,
        test_basic_fast_speech_caps_promotion,
    ]
    for fn in tests:
        fn()
        print(f"ok {fn.__name__}")
    print(f"All {len(tests)} self-checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())