from typing import Literal

from pydantic import BaseModel, Field

DictationLevel = Literal["A1", "A2", "B1", "B2", "C1", "C2"]
DictationStatus = Literal["draft", "published", "removed"]


class DictationSentence(BaseModel):
    orderIndex: int = Field(ge=0)
    text: str = Field(min_length=1)
    startTimeMs: int = Field(ge=0)
    endTimeMs: int = Field(gt=0)


class DictationVocabulary(BaseModel):
    word: str = Field(min_length=1)
    phonetic: str = ""
    meaning: str = ""
    exampleSentence: str = ""


class DictationLesson(BaseModel):
    id: str
    title: str
    level: DictationLevel
    source: str = ""
    sourceUrl: str = ""
    licenseNote: str = ""
    audioUrl: str
    durationSeconds: int | None = None
    sentences: list[DictationSentence] = Field(default_factory=list)
    vocabularies: list[DictationVocabulary] = Field(default_factory=list)
    updatedAt: str | None = None


class DictationLessonListResponse(BaseModel):
    level: DictationLevel | None
    page: int
    limit: int
    total: int
    totalPages: int
    items: list[DictationLesson]


class DictationLessonResponse(BaseModel):
    lesson: DictationLesson


class DictationVocabRequest(BaseModel):
    level: DictationLevel
    title: str = Field(min_length=1, max_length=200)
    transcript: str = Field(min_length=1)


class DictationVocabResponse(BaseModel):
    vocabularies: list[DictationVocabulary]
