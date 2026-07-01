import json
import logging

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


# Simon's band 9 philosophy: keep it linear, clear, cohesive, and simple.
# We embed a brief paragraph example directly in the system prompt so the model sees the desired voice.
# The JSON schema is also embedded in plain English so 9router / non-OpenAI providers
# (which often don't support native structured outputs) still return a parseable object.
SIMON_BAND9_SYSTEM_PROMPT = """You are "Simon", an ex-IELTS examiner and a highly regarded writing tutor. \
Your job is to evaluate a user's IELTS Writing Task essay and return a strict JSON object. Do not output any text outside the JSON object. No markdown, no prose, no commentary.

Your core teaching philosophy, derived from real Band 9 essays:
1. Linear structure: one clear main idea per paragraph. Introduction -> Body 1 -> Body 2 -> Conclusion.
2. Clear: every sentence serves a purpose. No padding, no rhetorical questions, no over-generalised lists.
3. Cohesive: use a small set of linking devices accurately ("Firstly", "In addition", "However", "As a result") rather than over-stuffing connectives.
4. Simple: prefer short, well-controlled sentences. Avoid complex, convoluted, or "show-off" syntax. Complex grammar (conditionals, passive voice, relative clauses) is fine only when it is clearly accurate.

Example of a Simon-style clear and cohesive paragraph (Band 9 tone):
"However, the main drawback is that employees who work from home often feel isolated from their colleagues. As a result, communication within the team can become slower and less effective. To deal with this issue, companies should organise regular online meetings and occasional in-person team events."

You MUST return a single JSON object with EXACTLY these four keys (no extra keys, no missing keys):
{
  "overall_band": <float between 0.0 and 9.0, in 0.5 increments, e.g. 6.5>,
  "coherence_feedback": <string, 2-4 sentences explaining what works and what is missing in structure / cohesion / paragraphing>,
  "vocabulary_suggestions": <array of 3-6 specific, drop-in word or phrase replacements that would lift the band>,
  "simon_style_rewrite": <string, a Band 9 style rewrite of the entire essay following the philosophy above>
}

Return ONLY the JSON object. No code fences. No explanation. No trailing commentary."""


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


async def evaluate_essay_with_ai(task_prompt: str, essay_text: str) -> WritingEvaluation:
    """Call the LLM and return a strongly-typed WritingEvaluation.

    Uses the standard `chat.completions.create` endpoint with `response_format={"type": "json_object"}`
    (basic JSON mode). This is supported by both OpenAI and most OpenAI-compatible proxies
    like 9router, which usually do NOT support the newer `beta.chat.completions.parse`
    structured-outputs endpoint.

    The response is manually parsed into the Pydantic `WritingEvaluation` model, giving us
    a hard schema guarantee (overall_band clamped 0.0-9.0, required fields, list[str], etc.).
    """
    client = get_llm_client()
    user_message = (
        f"Task prompt:\n{task_prompt.strip()}\n\n"
        f"User's essay:\n{essay_text.strip()}\n\n"
        "Return the evaluation strictly as the required JSON object."
    )

    response = await client.chat.completions.create(
        model=settings.llm_model,
        messages=[
            {"role": "system", "content": SIMON_BAND9_SYSTEM_PROMPT},
            {"role": "user", "content": user_message},
        ],
        response_format={"type": "json_object"},
        temperature=0.3,
    )

    raw = response.choices[0].message.content or ""
    logger.debug("LLM raw response: %s", raw)

    try:
        json_text = _extract_json_object(raw)
        payload = json.loads(json_text)
    except (ValueError, json.JSONDecodeError) as e:
        raise RuntimeError(
            f"LLM returned a non-JSON response. First 200 chars: {raw[:200]!r}"
        ) from e

    try:
        return WritingEvaluation.model_validate(payload)
    except ValidationError as e:
        raise RuntimeError(
            f"LLM JSON did not match the WritingEvaluation schema. Errors: {e.errors()}"
        ) from e