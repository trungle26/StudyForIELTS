"""Regression test for FastAPI OpenAPI generation and shared dictation metadata."""
from __future__ import annotations

from app.main import app


def main() -> None:
    schema = app.openapi()
    assert schema["openapi"]

    schemas = schema["components"]["schemas"]
    lesson = schemas["DictationLesson"]
    properties = lesson["properties"]

    assert {"contentType", "skill", "cefrLevel", "sourceUrl", "licenseNote", "tags"} <= properties.keys()
    assert "DictationLesson" in schemas
    assert "DictationLessonListResponse" in schemas
    assert "DictationLessonResponse" in schemas


if __name__ == "__main__":
    main()
