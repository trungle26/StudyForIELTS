"""Focused serialization compatibility checks for shared dictation metadata."""
from __future__ import annotations

from app.models.dictation import DictationLesson


def main() -> None:
    legacy = DictationLesson(id="1", title="Legacy", level="B1", audioUrl="audio.mp3")
    assert legacy.contentType == "dictation"
    assert legacy.cefrLevel is None
    assert legacy.model_dump()["level"] == "B1"

    shared = DictationLesson(
        id="2",
        title="Shared",
        level="B2",
        cefrLevel="B1",
        contentType="listening",
        skill="listening",
        tags=["daily routines"],
    )
    payload = shared.model_dump()
    assert payload["level"] == "B2"
    assert payload["cefrLevel"] == "B1"
    assert payload["contentType"] == "listening"
    assert payload["tags"] == ["daily routines"]


if __name__ == "__main__":
    main()
