import json
import logging
from pathlib import Path
from typing import Optional
from fastapi import APIRouter, HTTPException, Query
from app.models.strategy import StrategyGuide, StrategyListResponse, IeltsSkill

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/strategies", tags=["Strategies & Tips"])

_DATA_FILE = Path(__file__).resolve().parent.parent.parent / "seed_strategies.json"
_CACHED_STRATEGIES: list[StrategyGuide] = []


def _load_strategies() -> list[StrategyGuide]:
    global _CACHED_STRATEGIES
    if not _CACHED_STRATEGIES and _DATA_FILE.exists():
        try:
            with open(_DATA_FILE, "r", encoding="utf-8") as f:
                raw_data = json.load(f)
                _CACHED_STRATEGIES = [StrategyGuide.model_validate(item) for item in raw_data]
        except Exception as e:
            logger.error(f"Failed to load seed strategies: {e}")
            _CACHED_STRATEGIES = []
    return _CACHED_STRATEGIES


@router.get("", response_model=StrategyListResponse)
async def list_strategies(
    skill: Optional[IeltsSkill] = Query(None, description="Filter by IELTS Skill"),
    question_type: Optional[str] = Query(None, description="Filter by question type"),
):
    """List all available IELTS strategy guides, optionally filtered by skill or question type."""
    strategies = _load_strategies()
    filtered = strategies
    if skill:
        filtered = [s for s in filtered if s.skill == skill]
    if question_type:
        filtered = [s for s in filtered if s.question_type == question_type]

    return StrategyListResponse(items=filtered, total=len(filtered))


@router.get("/spotlight", response_model=StrategyGuide)
async def get_spotlight_strategy():
    """Get the spotlight/featured strategy for the day."""
    strategies = _load_strategies()
    spotlights = [s for s in strategies if s.is_spotlight]
    if spotlights:
        # Return the first spotlight item (or could cycle by date)
        return spotlights[0]
    if strategies:
        return strategies[0]
    raise HTTPException(status_code=404, detail="No strategies available")


@router.get("/{strategy_id}", response_model=StrategyGuide)
async def get_strategy_detail(strategy_id: str):
    """Retrieve full details of a specific strategy guide."""
    strategies = _load_strategies()
    for item in strategies:
        if item.id == strategy_id:
            return item
    raise HTTPException(status_code=404, detail=f"Strategy '{strategy_id}' not found")
