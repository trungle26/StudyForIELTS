import json
import logging
from pathlib import Path
from typing import AsyncIterator

from openai import AsyncOpenAI
from pydantic import ValidationError

from app.core.config import settings
from app.models.writing import WritingEvaluation

logger = logging.getLogger(__name__)


# Lazily initialized async OpenAI client (compatible with 9router's OpenAI-compatible base URL).
_client: AsyncOpenAI | None = None


def get_llm_client() -> AsyncOpenAI:
    """Return a singleton AsyncOpenAI client bound to 9router (or any OpenAI-compatible endpoint)."""
    global _client
    if _client is None:
        if not settings.llm_api_key:
            raise RuntimeError("LLM_API_KEY is required to instantiate the LLM client.")
        _client = AsyncOpenAI(
            api_key=settings.llm_api_key,
            base_url=settings.llm_base_url or None,
        )
    return _client


# ponytail: prompt loaded from plain .txt file; upgrade to Jinja templates if
# you ever need per-request variable interpolation inside the system prompt.
_PROMPTS_DIR = Path(__file__).resolve().parent.parent / "prompts"
ACTIVE_PROMPT_VERSION = "v2"
SIMON_BAND9_SYSTEM_PROMPT = (_PROMPTS_DIR / f"writing_{ACTIVE_PROMPT_VERSION}.txt").read_text()

_MAX_VALIDATION_RETRIES = 2  # up to 3 total attempts


def _extract_json_object(text: str) -> str:
    """Pull a JSON object out of a string that might contain extra prose / markdown fences.

    Some providers ignore the "no markdown" instruction and wrap the JSON in ```json ... ```.
    This helper strips the wrappers and finds the first { ... } block.
    """
    text = text.strip()
    # Strip leading/trailing markdown code fences if present.
    if text.startswith("```"):
        # Drop the first line (```json or ```) and the trailing ```
        lines = text.splitlines()
        if lines and lines[0].startswith("```"):
            lines = lines[1:]
        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]
        text = "\n".join(lines).strip()
    # Find the first '{' and the last matching '}'.
    start = text.find("{")
    end = text.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise ValueError(f"LLM response did not contain a JSON object. Got: {text[:200]!r}")
    return text[start : end + 1]


def _build_user_message(task_prompt: str, essay_text: str) -> str:
    """Build the user message with essay delimiters for injection defense."""
    return (
        f"Task prompt:\n{task_prompt.strip()}\n\n"
        f"User's essay:\n<<<ESSAY_START>>>\n{essay_text.strip()}\n<<<ESSAY_END>>>\n\n"
        "Return the evaluation strictly as the required JSON object."
    )


def _check_suspicious_score(essay_text: str, evaluation: WritingEvaluation) -> None:
    """Log a warning if a very short essay gets a suspiciously high band."""
    if len(essay_text.split()) < 100 and evaluation.overall_band >= 8.5:
        logger.warning(
            "Suspicious score: %s-word essay scored %.1f — possible prompt injection",
            len(essay_text.split()),
            evaluation.overall_band,
        )


def _parse_llm_response(raw: str) -> WritingEvaluation:
    """Extract JSON from raw LLM text and validate against the Pydantic model."""
    json_text = _extract_json_object(raw)
    payload = json.loads(json_text)
    return WritingEvaluation.model_validate(payload)


async def evaluate_essay_with_ai(task_prompt: str, essay_text: str) -> WritingEvaluation:
    """Call the LLM and return a strongly-typed WritingEvaluation.

    Retries up to ``_MAX_VALIDATION_RETRIES`` times when the LLM returns JSON
    that fails Pydantic validation, feeding the error back to the model so it
    can self-correct.  On final failure raises ``RuntimeError`` with a
    client-friendly message (the router maps this to 502).
    """
    client = get_llm_client()
    user_message = _build_user_message(task_prompt, essay_text)

    messages: list[dict[str, str]] = [
        {"role": "system", "content": SIMON_BAND9_SYSTEM_PROMPT},
        {"role": "user", "content": user_message},
    ]

    last_error: Exception | None = None
    for attempt in range(_MAX_VALIDATION_RETRIES + 1):
        response = await client.chat.completions.create(
            model=settings.llm_model,
            messages=messages,
            response_format={"type": "json_object"},
            temperature=0.3,
        )
        raw = response.choices[0].message.content or ""
        logger.debug("LLM raw response (attempt %d): %s", attempt + 1, raw)

        try:
            evaluation = _parse_llm_response(raw)
            _check_suspicious_score(essay_text, evaluation)
            return evaluation
        except (ValueError, json.JSONDecodeError, ValidationError) as e:
            last_error = e
            logger.warning("LLM validation failed (attempt %d/%d): %s", attempt + 1, _MAX_VALIDATION_RETRIES + 1, e)
            if attempt < _MAX_VALIDATION_RETRIES:
                # Feed the error back so the model can self-correct.
                messages.append({"role": "assistant", "content": raw})
                messages.append({
                    "role": "user",
                    "content": (
                        f"Your previous response failed validation: {e}. "
                        "Correct your output to match the required JSON schema exactly, with no other text."
                    ),
                })

    raise RuntimeError("grading_temporarily_unavailable") from last_error


async def stream_essay_evaluation(
    task_prompt: str,
    essay_text: str,
) -> AsyncIterator[str]:
    """Yield the LLM's raw text deltas, then a final validated ``WritingEvaluation``.

    After the stream completes, validates the accumulated JSON.  If validation
    fails, makes ONE non-streaming retry with the error fed back to the model.
    """
    client = get_llm_client()
    user_message = _build_user_message(task_prompt, essay_text)

    messages: list[dict[str, str]] = [
        {"role": "system", "content": SIMON_BAND9_SYSTEM_PROMPT},
        {"role": "user", "content": user_message},
    ]

    accumulated: list[str] = []
    try:
        stream = await client.chat.completions.create(
            model=settings.llm_model,
            messages=messages,
            response_format={"type": "json_object"},
            temperature=0.3,
            stream=True,
            timeout=settings.llm_request_timeout_seconds,
        )
        async for chunk in stream:
            try:
                delta = chunk.choices[0].delta.content or ""
            except (IndexError, AttributeError):
                delta = ""
            if not delta:
                continue
            accumulated.append(delta)
            yield f"data: {delta}\n\n"
    except Exception as e:  # noqa: BLE001 - surface any LLM-side error to the client
        logger.exception("Streaming LLM call failed")
        yield f"event: error\ndata: {str(e)}\n\n"
        return

    raw = "".join(accumulated)
    logger.debug("LLM streamed raw response: %s", raw)

    # --- Validate, with one non-streaming retry on failure ---
    try:
        evaluation = _parse_llm_response(raw)
    except (ValueError, json.JSONDecodeError, ValidationError) as first_err:
        logger.warning("Streamed response validation failed, attempting retry: %s", first_err)
        try:
            messages.append({"role": "assistant", "content": raw})
            messages.append({
                "role": "user",
                "content": (
                    f"Your previous response failed validation: {first_err}. "
                    "Correct your output to match the required JSON schema exactly, with no other text."
                ),
            })
            retry_resp = await client.chat.completions.create(
                model=settings.llm_model,
                messages=messages,
                response_format={"type": "json_object"},
                temperature=0.3,
            )
            retry_raw = retry_resp.choices[0].message.content or ""
            evaluation = _parse_llm_response(retry_raw)
        except Exception as retry_err:  # noqa: BLE001
            logger.warning("Streaming retry also failed: %s", retry_err)
            yield f"event: error\ndata: grading_temporarily_unavailable\n\n"
            return

    _check_suspicious_score(essay_text, evaluation)
    yield f"event: done\ndata: {evaluation.model_dump_json()}\n\n"
