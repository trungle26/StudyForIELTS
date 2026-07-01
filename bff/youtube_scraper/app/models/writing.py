from datetime import datetime
from pydantic import BaseModel, Field


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