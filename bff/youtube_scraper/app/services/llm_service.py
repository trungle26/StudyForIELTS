import base64
import json
import logging
from dataclasses import dataclass
from pathlib import Path
from typing import Any, AsyncIterator

from openai import AsyncOpenAI
from openai.types.chat import ChatCompletionMessageParam
from pydantic import ValidationError

from app.core.config import settings
from app.models.dictation import DictationVocabulary
from app.models.writing import WritingEvaluation

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class EvaluationResult:
    """Evaluation plus optional token usage from the LLM provider.

    Token counts are None when the provider does not return them (some
    9router-routed models omit usage data). The router is responsible for
    deciding whether to persist the cost fields as null.
    """
    evaluation: WritingEvaluation
    input_tokens: int | None = None
    output_tokens: int | None = None


def _extract_usage(response: object) -> tuple[int | None, int | None]:
    """Pull prompt/completion tokens off an OpenAI-compatible response.

    Returns (None, None) when the provider doesn't return usage data; never
    raises — token logging is best-effort.
    """
    usage = getattr(response, "usage", None)
    if usage is None:
        return None, None
    input_tokens = getattr(usage, "prompt_tokens", None)
    output_tokens = getattr(usage, "completion_tokens", None)
    return input_tokens, output_tokens


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

# Task 1 (Academic) uses its own versioned prompt — same JSON schema, Task Achievement rubric.
ACTIVE_TASK1_PROMPT_VERSION = "v1"
SIMON_BAND9_TASK1_SYSTEM_PROMPT = (_PROMPTS_DIR / f"writing_task1_{ACTIVE_TASK1_PROMPT_VERSION}.txt").read_text()

DICTATION_VOCAB_PROMPT_VERSION = "v1"
DICTATION_VOCAB_SYSTEM_PROMPT = (_PROMPTS_DIR / f"dictation_vocab_{DICTATION_VOCAB_PROMPT_VERSION}.txt").read_text()

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

def _sniff_image_media_type(image_bytes: bytes) -> str:
    """Best-effort sniff of common image formats. Defaults to image/png when unknown."""
    if image_bytes.startswith(b"\x89PNG\r\n\x1a\n"):
        return "image/png"
    if image_bytes[:3] == b"\xff\xd8\xff":
        return "image/jpeg"
    if image_bytes[:6] in (b"GIF87a", b"GIF89a"):
        return "image/gif"
    if image_bytes[:4] == b"RIFF" and image_bytes[8:12] == b"WEBP":
        return "image/webp"
    return "image/png"

def _build_task1_user_message(task_prompt: str, essay_text: str, image_bytes: bytes) -> list[dict]:
    """OpenAI-compatible multimodal user content for Task 1: text + chart image as data URI."""
    media_type = _sniff_image_media_type(image_bytes)
    b64 = base64.b64encode(image_bytes).decode("ascii")
    return [
        {
            "type": "text",
            "text": (
                f"Task prompt:\n{task_prompt.strip()}\n\n"
                "Chart image (analyse the visible data, not assumed content):\n"
                f"<<<IMAGE_START>>>\ndata:{media_type};base64,{b64}\n<<<IMAGE_END>>>\n\n"
                "User's essay (the only data you grade against the image):\n"
                f"<<<ESSAY_START>>>\n{essay_text.strip()}\n<<<ESSAY_END>>>\n\n"
                "Return the evaluation strictly as the required JSON object."
            ),
        },
        {
            "type": "image_url",
            # ponytail: data URIs are fine for our chart-size images (<=8 MB upload cap
            # from 3.2). Upgrade to OpenAI Files API when chart sizes regularly exceed
            # provider data-URI limits.
            "image_url": {"url": f"data:{media_type};base64,{b64}"},
        },
    ]


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


async def _run_with_validation_retries(
    *,
    system_prompt: str,
    initial_user_content,
    model: str,
) -> EvaluationResult:
    """Shared validation-retry loop used by both Task 1 and Task 2 grading.

    ``initial_user_content`` is either a plain string (Task 2) or a list of
    content parts (Task 1, multimodal). The retry/usage-accumulation behavior
    is identical to the previous inline Task 2 implementation; the message
    shape is the only thing that varies.
    """
    client = get_llm_client()
    messages: list[ChatCompletionMessageParam] = [
        {"role": "system", "content": system_prompt},
        # ChatCompletionMessageParam's user content is `str | Iterable[...]`;
        # cast through Any to keep the OpenAI SDK happy with both shapes.
        {"role": "user", "content": initial_user_content},  # type: ignore[typeddict-item]
    ]

    last_error: Exception | None = None
    total_input_tokens: int | None = 0
    total_output_tokens: int | None = 0
    for attempt in range(_MAX_VALIDATION_RETRIES + 1):
        response = await client.chat.completions.create(
            model=model,
            messages=messages,
            response_format={"type": "json_object"},
            temperature=0.3,
        )
        raw = response.choices[0].message.content or ""
        logger.debug("LLM raw response (attempt %d, model=%s): %s", attempt + 1, model, raw)

        in_tok, out_tok = _extract_usage(response)
        if in_tok is not None:
            total_input_tokens = (total_input_tokens or 0) + in_tok
        if out_tok is not None:
            total_output_tokens = (total_output_tokens or 0) + out_tok

        try:
            evaluation = _parse_llm_response(raw)
            return EvaluationResult(
                evaluation=evaluation,
                input_tokens=total_input_tokens or None,
                output_tokens=total_output_tokens or None,
            )
        except (ValueError, json.JSONDecodeError, ValidationError) as e:
            last_error = e
            logger.warning("LLM validation failed (attempt %d/%d, model=%s): %s", attempt + 1, _MAX_VALIDATION_RETRIES + 1, model, e)
            if attempt < _MAX_VALIDATION_RETRIES:
                messages.append({"role": "assistant", "content": raw})
                messages.append({
                    "role": "user",
                    "content": (
                        f"Your previous response failed validation: {e}. "
                        "Correct your output to match the required JSON schema exactly, with no other text."
                    ),
                })

    raise RuntimeError("grading_temporarily_unavailable") from last_error

async def evaluate_essay_with_ai(task_prompt: str, essay_text: str) -> EvaluationResult:
    """Call the LLM and return an ``EvaluationResult`` with optional token usage.

    Retries up to ``_MAX_VALIDATION_RETRIES`` times when the LLM returns JSON
    that fails Pydantic validation, feeding the error back to the model so it
    can self-correct. Token counts are summed across attempts (each retry is
    a real LLM call). On final failure raises ``RuntimeError`` with a
    client-friendly message (the router maps this to 502).
    """
    user_message = _build_user_message(task_prompt, essay_text)
    result = await _run_with_validation_retries(
        system_prompt=SIMON_BAND9_SYSTEM_PROMPT,
        initial_user_content=user_message,
        model=settings.llm_model,
    )
    _check_suspicious_score(essay_text, result.evaluation)
    return result

async def evaluate_task1_essay_with_ai(
    task_prompt: str,
    essay_text: str,
    image_bytes: bytes,
) -> EvaluationResult:
    """Grade a Task 1 (Academic) essay against an attached chart image.

    Builds an OpenAI-compatible multimodal user message (text + ``image_url``
    data URI), uses the Task 1 system prompt, and runs the same validation
    retry loop as Task 2. The vision model is configurable independently of
    the text model via ``LLM_VISION_MODEL`` (falls back to ``LLM_MODEL``).
    The service layer is pure — image bytes come from the router/GridFS.
    """
    if not image_bytes:
        raise ValueError("image_bytes must be non-empty for Task 1 evaluation")
    user_content = _build_task1_user_message(task_prompt, essay_text, image_bytes)
    result = await _run_with_validation_retries(
        system_prompt=SIMON_BAND9_TASK1_SYSTEM_PROMPT,
        initial_user_content=user_content,
        model=settings.llm_vision_model,
    )
    _check_suspicious_score(essay_text, result.evaluation)
    return result


async def generate_dictation_vocabulary(
    level: str,
    title: str,
    transcript: str,
) -> list[DictationVocabulary]:
    """Ask the configured LLM to pick listening vocabulary from a transcript.

    Reuses the OpenAI-compatible client and ``response_format=json_object``
    contract used by the writing graders, with a dedicated system prompt that
    pins the output schema to ``{"vocabularies": [{word, phonetic, meaning,
    exampleSentence}, ...]}``. Validates with Pydantic and retries up to
    ``_MAX_VALIDATION_RETRIES`` times on schema failure. Raises
    ``RuntimeError("vocab_temporarily_unavailable")`` after the final failure
    so the router maps it to a 502.
    """
    user_content = (
        f"Lesson level: {level}\n"
        f"Lesson title: {title}\n"
        f"Transcript:\n<<<TRANSCRIPT_START>>>\n{transcript.strip()}\n<<<TRANSCRIPT_END>>>\n\n"
        "Return ONLY the required JSON object."
    )
    client = get_llm_client()
    messages: list[ChatCompletionMessageParam] = [
        {"role": "system", "content": DICTATION_VOCAB_SYSTEM_PROMPT},
        {"role": "user", "content": user_content},
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
        logger.debug("Dictation vocab raw response (attempt %d): %s", attempt + 1, raw)
        try:
            payload = json.loads(_extract_json_object(raw))
            items = payload.get("vocabularies")
            if not isinstance(items, list):
                raise ValueError("LLM response missing 'vocabularies' list.")
            return [DictationVocabulary.model_validate(item) for item in items]
        except (ValueError, json.JSONDecodeError, ValidationError) as e:
            last_error = e
            logger.warning("Dictation vocab validation failed (attempt %d/%d): %s", attempt + 1, _MAX_VALIDATION_RETRIES + 1, e)
            if attempt < _MAX_VALIDATION_RETRIES:
                messages.append({"role": "assistant", "content": raw})
                messages.append({
                    "role": "user",
                    "content": (
                        f"Your previous response failed validation: {e}. "
                        "Correct your output to match the required JSON schema exactly, with no other text."
                    ),
                })
    raise RuntimeError("vocab_temporarily_unavailable") from last_error


async def _stream_evaluation_with_retry(
    *,
    system_prompt: str,
    initial_user_content,
    model: str,
    essay_text: str,
) -> AsyncIterator[str]:
    """Shared SSE stream for both Task 1 and Task 2 grading.

    ``initial_user_content`` is a plain string for Task 2 or a list of
    multimodal parts for Task 1; only the message shape varies. The stream
    shape (raw ``data:`` deltas, one ``event: usage`` line, one
    ``event: done`` line on success, ``event: error`` on failure) is
    identical so the Android client only needs one parser.
    """
    client = get_llm_client()
    messages: list[dict[str, Any]] = [
        {"role": "system", "content": system_prompt},
        # See _run_with_validation_retries: same OpenAI SDK typing workaround.
        {"role": "user", "content": initial_user_content},  # type: ignore[typeddict-item]
    ]

    accumulated: list[str] = []
    input_tokens: int | None = None
    output_tokens: int | None = None
    try:
        stream = await client.chat.completions.create(
            model=model,
            messages=messages,
            response_format={"type": "json_object"},
            temperature=0.3,
            stream=True,
            timeout=settings.llm_request_timeout_seconds,
            # Ask the provider to emit a final usage chunk; some 9router-routed
            # models ignore this and just return None usage on the final chunk.
            stream_options={"include_usage": True},
        )
        async for chunk in stream:
            # Final chunk with usage only: no delta content, but may carry usage.
            if not getattr(chunk, "choices", None):
                u_in, u_out = _extract_usage(chunk)
                if u_in is not None:
                    input_tokens = u_in
                if u_out is not None:
                    output_tokens = u_out
                continue
            try:
                delta = chunk.choices[0].delta.content or ""
            except (IndexError, AttributeError):
                delta = ""
            if not delta:
                # Even on content-less intermediate chunks, usage may be present.
                u_in, u_out = _extract_usage(chunk)
                if u_in is not None:
                    input_tokens = u_in
                if u_out is not None:
                    output_tokens = u_out
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
                model=model,
                messages=messages,
                response_format={"type": "json_object"},
                temperature=0.3,
            )
            retry_raw = retry_resp.choices[0].message.content or ""
            r_in, r_out = _extract_usage(retry_resp)
            if r_in is not None:
                input_tokens = (input_tokens or 0) + r_in
            if r_out is not None:
                output_tokens = (output_tokens or 0) + r_out
            evaluation = _parse_llm_response(retry_raw)
        except Exception as retry_err:  # noqa: BLE001
            logger.warning("Streaming retry also failed: %s", retry_err)
            yield f"event: error\ndata: grading_temporarily_unavailable\n\n"
            return

    _check_suspicious_score(essay_text, evaluation)
    # Emit usage event right before done so the router can pick it up when
    # parsing the stream for persistence. The client may ignore unknown events.
    usage_payload = json.dumps({"input_tokens": input_tokens, "output_tokens": output_tokens})
    yield f"event: usage\ndata: {usage_payload}\n\n"
    yield f"event: done\ndata: {evaluation.model_dump_json()}\n\n"

async def stream_essay_evaluation(
    task_prompt: str,
    essay_text: str,
) -> AsyncIterator[str]:
    """Yield the LLM's raw text deltas, then a final validated ``WritingEvaluation``.

    Thin wrapper around ``_stream_evaluation_with_retry`` for Task 2. The
    stream shape and event protocol are identical to
    ``evaluate_task1_essay_with_ai_stream`` so the Android client only
    needs one parser.
    """
    user_message = _build_user_message(task_prompt, essay_text)
    async for raw_event in _stream_evaluation_with_retry(
        system_prompt=SIMON_BAND9_SYSTEM_PROMPT,
        initial_user_content=user_message,
        model=settings.llm_model,
        essay_text=essay_text,
    ):
        yield raw_event

async def evaluate_task1_essay_with_ai_stream(
    task_prompt: str,
    essay_text: str,
    image_bytes: bytes,
) -> AsyncIterator[str]:
    """Stream a Task 1 (Academic) essay grading as Server-Sent Events.

    Same event protocol as ``stream_essay_evaluation``: raw ``data:`` deltas,
    one ``event: usage`` line with token counts, and one ``event: done`` line
    carrying the final ``WritingEvaluation`` JSON. The vision model is
    configurable via ``LLM_VISION_MODEL`` and is the only difference from the
    Task 2 stream (besides the multimodal user content).
    """
    if not image_bytes:
        yield f"event: error\ndata: image_required_for_task1\n\n"
        return
    user_content = _build_task1_user_message(task_prompt, essay_text, image_bytes)
    async for raw_event in _stream_evaluation_with_retry(
        system_prompt=SIMON_BAND9_TASK1_SYSTEM_PROMPT,
        initial_user_content=user_content,
        model=settings.llm_vision_model,
        essay_text=essay_text,
    ):
        yield raw_event
