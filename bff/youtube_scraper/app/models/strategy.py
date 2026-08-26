from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field


class IeltsSkill(str, Enum):
    LISTENING = "listening"
    READING = "reading"
    WRITING_TASK1 = "writing_task1"
    WRITING_TASK2 = "writing_task2"
    SPEAKING = "speaking"


class StrategyStep(BaseModel):
    step_number: int
    title: str
    description: str


class StrategyGuide(BaseModel):
    id: str
    skill: IeltsSkill
    question_type: str
    question_type_label: str
    title: str
    summary: str
    target_band: str = "7.0+"
    time_budget_min: int = 20
    is_spotlight: bool = False
    overview: str
    steps: List[StrategyStep] = Field(default_factory=list)
    dos: List[str] = Field(default_factory=list)
    donts: List[str] = Field(default_factory=list)
    common_traps: List[str] = Field(default_factory=list)
    golden_rule: Optional[str] = None


class StrategyListResponse(BaseModel):
    items: List[StrategyGuide]
    total: int
