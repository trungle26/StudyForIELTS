from datetime import datetime
from typing import Literal
from uuid import uuid4

from pydantic import BaseModel, Field


TaskType = Literal["task1", "task2"]
LessonDifficulty = Literal["easy", "medium", "hard"]
LessonStatus = Literal["draft", "published"]


def _new_lesson_id() -> str:
    return str(uuid4())


class EssaySubmission(BaseModel):
    task_prompt: str = Field(..., description="The essay prompt or question that the user is responding to.")
    essay_text: str = Field(..., description="The essay text written by the user to be evaluated.")


class WritingEvaluation(BaseModel):
    overall_band: float = Field(
        ...,
        description="The estimated overall band score on a scale of 0 to 9.0 (can be in increments of 0.5 like 7.5).",
        ge=0.0,
        le=9.0,
    )
    coherence_feedback: str = Field(
        ...,
        description="Feedback focusing on cohesion, structure, progression, paragraphing, and logical flow of arguments.",
    )
    vocabulary_suggestions: list[str] = Field(
        ...,
        description="A list of specific vocabulary suggestions, replacements, or collocations to elevate the vocabulary range.",
    )
    simon_style_rewrite: str = Field(
        ...,
        description="A rewrite of the essay or sections of the essay adopting 'Simon's Band 9 style' (linear, clear, simple, and highly cohesive structures).",
    )


class WritingEvaluationDB(WritingEvaluation):
    id: str = Field(..., description="A unique UUID string identifying the writing evaluation record.")
    task_prompt: str = Field(..., description="The essay prompt or question that the user responded to.")
    essay_text: str = Field(..., description="The essay text written by the user.")
    created_at: datetime = Field(..., description="The timestamp when this evaluation was created (UTC).")
    # Token usage from the LLM provider (None if the provider didn't return usage data).
    input_tokens: int | None = Field(default=None, description="Prompt tokens consumed for this evaluation.")
    output_tokens: int | None = Field(default=None, description="Completion tokens consumed for this evaluation.")
    estimated_cost_usd: float | None = Field(
        default=None, description="Estimated USD cost computed from token counts and configured pricing."
    )


# --- Priority 3.1: Writing lessons (admin-curated Task 1/Task 2 prompts) ---


class WritingLesson(BaseModel):
    """A single writing lesson as stored in MongoDB.

    ``image_id`` is a GridFS file id for Task 1 chart/graph images and null
    for Task 2 lessons.
    """
    id: str = Field(default_factory=_new_lesson_id, description="UUID identifying this lesson.")
    task_type: TaskType = Field(..., description="Which IELTS writing task this lesson targets.")
    task_prompt: str = Field(..., min_length=1, description="The prompt the student writes against.")
    image_id: str | None = Field(default=None, description="GridFS file id of the chart image (Task 1 only).")
    sample_answer: str = Field(..., min_length=1, description="Band 9 model answer shown to students.")
    tips: list[str] = Field(default_factory=list, description="Short, ordered study tips for this lesson.")
    difficulty: LessonDifficulty | None = Field(default=None, description="Optional difficulty tag.")
    status: LessonStatus = Field(default="draft", description="Visibility flag; only 'published' is exposed publicly.")
    created_at: datetime = Field(..., description="UTC timestamp the lesson was first created.")
    updated_at: datetime = Field(..., description="UTC timestamp the lesson was last modified.")


class WritingLessonResponse(WritingLesson):
    """Public-facing shape; identical to the DB model for now (kept as a
    separate type so internal fields can be added later without breaking the
    Android contract)."""
    pass


class WritingLessonListResponse(BaseModel):
    """Paginated list response for ``GET /writing/lessons``."""
    page: int = Field(..., ge=1, description="1-based page number.")
    limit: int = Field(..., ge=1, description="Page size actually applied (after clamping).")
    total: int = Field(..., ge=0, description="Total number of matching published lessons.")
    total_pages: int = Field(..., ge=0, description="Total number of pages at the current page size.")
    items: list[WritingLessonResponse] = Field(default_factory=list)