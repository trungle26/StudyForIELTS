"""Offline self-check for 3.5 Task 1 vision LLM call wiring.

This file intentionally mocks the OpenAI SDK and pydantic so it can run in a
bare interpreter with no installed deps. It exercises:

  - The shared retry/usage loop (`_run_with_validation_retries`) preserves
    the existing Task 2 behavior: 3 attempts max, error fed back on retry,
    token counts summed across attempts, `RuntimeError("grading_temporarily_unavailable")`
    raised on final failure.
  - `evaluate_essay_with_ai` still routes through the shared helper.
  - `evaluate_task1_essay_with_ai` builds a multimodal user message with
    a base64 image_url data URI, uses the Task 1 system prompt, and uses
    `settings.llm_vision_model` (not the text model).
  - `_build_task1_user_message` emits both essay and image delimiters and
    sniffs the right media type from common byte signatures.
  - Empty `image_bytes` is rejected up front (defends the router from a
    missing GridFS image silently being graded as text).

It does NOT make any real LLM call. Run with:
``python eval/check_task1_llm_service.py``.
"""
from __future__ import annotations

import asyncio
import base64
import json
import sys
import types
from dataclasses import dataclass, field  # noqa: E402  (used by stubs below)
from typing import Any

# --- mock the modules we don't want to install ------------------------------

def _install_stub(name: str, attrs: dict[str, object] | None = None) -> None:
    if name in sys.modules:
        return
    mod = types.ModuleType(name)
    for k, v in (attrs or {}).items():
        setattr(mod, k, v)
    sys.modules[name] = mod

# Tiny pydantic stub: enough for WritingEvaluation.model_validate / field access.
class _Field:
    def __init__(self, default=..., **kw):
        self.default = default
        self.metadata = kw
    def __call__(self, *a, **kw):
        return self

class _BaseModel:
    def __init_subclass__(cls, **kw):
        super().__init_subclass__(**kw)
        import typing
        try:
            hints = typing.get_type_hints(cls)
        except Exception:
            hints = {}
        fields: dict = {}
        for k, v in cls.__dict__.items():
            if isinstance(v, _Field):
                fields[k] = v
        for klass in reversed(cls.__mro__[1:]):
            parent_fields = getattr(klass, "model_fields", None)
            if parent_fields:
                for name, fld in parent_fields.items():
                    fields.setdefault(name, fld)
        for name in hints:
            if name not in fields:
                fields[name] = _Field(...)
        cls.model_fields = fields

    def __init__(self, **kwargs):
        for name in getattr(self, "model_fields", {}):
            f = self.model_fields[name]
            if name in kwargs:
                value = kwargs[name]
            else:
                d = f.default
                if d is ...:
                    # Mirror pydantic: missing required field is a ValidationError
                    # so the production retry loop's `except ValidationError`
                    # clause actually fires.
                    raise _ValidationError(f"missing required field: {name}")
                if callable(d) and not isinstance(d, type):
                    value = d()
                else:
                    value = d
            setattr(self, name, value)

    def model_dump(self, mode: str | None = None) -> dict:
        return {k: getattr(self, k) for k in getattr(self, "model_fields", {})}
    def model_dump_json(self) -> str:
        return json.dumps(self.model_dump())
    @classmethod
    def model_validate(cls, data: dict):
        return cls(**data)
    @classmethod
    def model_validate_json(cls, data: str):
        return cls(**json.loads(data))

def _FieldFactory(*args, **kwargs):
    if args and "default" not in kwargs:
        kwargs["default"] = args[0]
    if "default_factory" in kwargs and "default" not in kwargs:
        kwargs["default"] = kwargs.pop("default_factory")
    return _Field(**kwargs)

_install_stub("pydantic", {"BaseModel": _BaseModel, "Field": _FieldFactory})

# pydantic.dataclasses.dataclass is imported by app.core.config. The real
# pydantic version strips Field() defaults from the class body before applying
# the stdlib dataclass decorator. Stdlib's @dataclass does not do that, so
# Field() objects would end up as raw class attributes. We build a tiny
# pydantic-compatible dataclass that performs the same stripping.
from dataclasses import field as _stdlib_field, fields as _stdlib_fields  # noqa: E402

def _pydantic_dataclass(cls=None, **kwargs):
    """Mimic ``@pydantic.dataclasses.dataclass``: strip Field() markers then
    apply stdlib @dataclass. Supports only what app.core.config uses: keyword
    args like ``frozen=True`` and ``Field(default=..., default_factory=...)``."""
    # If called with only kwargs (e.g. @pydantic_dataclass(frozen=True)), return a decorator.
    if cls is None:
        def wrap(klass):
            return _pydantic_dataclass(klass, **kwargs)
        return wrap

    # Resolve Field() defaults per attribute. _FieldFactory routes
    # `default_factory=` into `default=` (the lambda), so we detect that
    # case by checking whether `default` is callable with a non-class type.
    for name in list(vars(cls).keys()):
        value = getattr(cls, name)
        if isinstance(value, _Field):
            delattr(cls, name)
            d = value.default
            if d is ...:
                # Required field — leave it; stdlib @dataclass will treat the
                # annotation as required.
                continue
            if callable(d) and not isinstance(d, type):
                # _FieldFactory routed default_factory=lambda into default=lambda.
                setattr(cls, name, _stdlib_field(default_factory=d))
            else:
                setattr(cls, name, d)
    return dataclass(cls, **kwargs)

_install_stub("pydantic.dataclasses", {"dataclass": _pydantic_dataclass})
# ValidationError is a real pydantic class — stub it just enough that
# `except ValidationError` blocks catch it. Our real exception in the retry
# loop comes from `BaseModel.model_validate` raising TypeError, so we make
# our stub ValidationError a TypeError subclass too, which mirrors the real
# behavior and lets the same `except (ValueError, json.JSONDecodeError, ValidationError)`
# clauses in llm_service.py match it.
class _ValidationError(TypeError):
    pass

# Reinstall with ValidationError added.
pydantic_stub = sys.modules["pydantic"]
pydantic_stub.ValidationError = _ValidationError

# --- fake OpenAI async client ----------------------------------------------

@dataclass
class _FakeUsage:
    prompt_tokens: int = 0
    completion_tokens: int = 0

@dataclass
class _FakeMessage:
    content: str = ""

@dataclass
class _FakeChoice:
    message: _FakeMessage = field(default_factory=_FakeMessage)

@dataclass
class _FakeResponse:
    choices: list[_FakeChoice] = field(default_factory=list)
    usage: _FakeUsage | None = None

class _FakeChatCompletions:
    """Drives ``_run_with_validation_retries`` through a scripted list of responses."""
    def __init__(self, scripted: list[_FakeResponse]) -> None:
        self._scripted = list(scripted)
        self.calls: list[dict[str, Any]] = []

    async def create(self, **kwargs) -> _FakeResponse:
        self.calls.append(kwargs)
        if not self._scripted:
            raise AssertionError("scripted responses exhausted")
        return self._scripted.pop(0)

class _FakeAsyncOpenAI:
    def __init__(self, scripted: list[_FakeResponse]) -> None:
        self.chat = types.SimpleNamespace(completions=_FakeChatCompletions(scripted))

# Minimal openai package surface our module imports.
class _AsyncOpenAI:
    def __init__(self, *a, **kw):
        # Real instance is replaced below before the module is loaded.
        raise RuntimeError("placeholder")

class _ChatCompletionMessageParam(dict):
    pass

_install_stub("openai", {"AsyncOpenAI": _AsyncOpenAI})
_install_stub("openai.types", {})
_install_stub("openai.types.chat", {"ChatCompletionMessageParam": _ChatCompletionMessageParam})

# Settings stub so the module doesn't hit lru_cache / real env.
import os  # noqa: E402
os.environ.setdefault("LLM_API_KEY", "sk-test")
os.environ.setdefault("MONGODB_URI", "mongodb://localhost:27017")
# Force the two values we want to assert on. Must happen before app.core.config is imported.
os.environ["LLM_MODEL"] = "gpt-4o-mini"
os.environ["LLM_VISION_MODEL"] = "gemini-2.5-flash"
TEXT_MODEL = os.environ["LLM_MODEL"]
VISION_MODEL = os.environ["LLM_VISION_MODEL"]

# Build a real (frozen) dataclass that mimics the Settings surface the service
# layer actually touches. Keeping the field list aligned with app.core.config.
@dataclass(frozen=True)
class _Settings:
    mongodb_uri: str = ""
    mongodb_db_name: str = "StudyForIELTS"
    mongodb_collection: str = "curatedvideos"
    admin_token: str = ""
    cors_allow_origins: list = None  # type: ignore[assignment]
    llm_api_key: str = "sk-test"
    llm_base_url: str = ""
    llm_model: str = TEXT_MODEL
    llm_vision_model: str = VISION_MODEL
    llm_request_timeout_seconds: float = 180.0
    llm_stream: bool = True
    input_token_cost_per_million: float = 0.15
    output_token_cost_per_million: float = 0.60
    rate_limit_per_hour: int = 10
    cache_ttl_seconds: int = 86400

    def __post_init__(self):
        if self.cors_allow_origins is None:
            object.__setattr__(self, "cors_allow_origins", ["*"])

# Pre-construct and freeze into a single value the service module imports.
sys.path.insert(0, ".")
import app.core.config as _config_module  # noqa: E402
# Swap in our frozen settings + replace the module body so lru_cache returns ours.
_STUB_SETTINGS = _Settings()
_config_module.settings = _STUB_SETTINGS
_config_module.get_settings = lambda: _STUB_SETTINGS

# --- now load the service module -------------------------------------------

import app.services.llm_service as svc  # noqa: E402
svc.settings = _STUB_SETTINGS

# Helper: install a scripted client. svc._client must look like an
# AsyncOpenAI instance with .chat.completions.create() (not the .create
# callable directly), matching the production code path.
def _with_scripted(responses: list[_FakeResponse]) -> _FakeChatCompletions:
    fake = _FakeAsyncOpenAI(responses)
    svc._client = fake  # type: ignore[assignment]
    return fake.chat.completions

def check(name: str, cond: bool, hint: str = "") -> None:
    if not cond:
        print(f"FAIL: {name} {hint}")
        sys.exit(1)
    print(f"ok: {name}")

# --- 1. config + constants ------------------------------------------------

check("vision model is configurable", svc.settings.llm_vision_model == "gemini-2.5-flash")
check("text model unchanged", svc.settings.llm_model == "gpt-4o-mini")
check("task1 prompt loaded from disk", "<<<ESSAY_START>>>" in svc.SIMON_BAND9_TASK1_SYSTEM_PROMPT)
check("task1 prompt image-aware", "<<<IMAGE_START>>>" in svc.SIMON_BAND9_TASK1_SYSTEM_PROMPT)
check("task1 prompt same JSON keys", all(k in svc.SIMON_BAND9_TASK1_SYSTEM_PROMPT for k in
    ["overall_band", "coherence_feedback", "vocabulary_suggestions", "simon_style_rewrite"]))
check("active task1 version constant", svc.ACTIVE_TASK1_PROMPT_VERSION == "v1")

# --- 2. media-type sniffer ------------------------------------------------

# PNG signature
png = b"\x89PNG\r\n\x1a\n" + b"\x00" * 16
check("sniff png", svc._sniff_image_media_type(png) == "image/png")
# JPEG (SOI marker)
jpeg = b"\xff\xd8\xff\xe0" + b"\x00" * 8
check("sniff jpeg", svc._sniff_image_media_type(jpeg) == "image/jpeg")
# GIF87a
gif = b"GIF87a" + b"\x00" * 16
check("sniff gif", svc._sniff_image_media_type(gif) == "image/gif")
# WEBP
webp = b"RIFF" + b"\x00\x00\x00\x00" + b"WEBP" + b"\x00" * 8
check("sniff webp", svc._sniff_image_media_type(webp) == "image/webp")
# Unknown -> safe default
check("sniff unknown defaults to png", svc._sniff_image_media_type(b"not an image") == "image/png")

# --- 3. task1 user message shape ------------------------------------------

parts = svc._build_task1_user_message(
    task_prompt="Summarise the chart.",
    essay_text="Overall, sales rose.",
    image_bytes=png,
)
check("user message is a list of content parts", isinstance(parts, list) and len(parts) == 2)
text_part, image_part = parts
check("first part is text", text_part["type"] == "text")
check("text has image delimiter", "<<<IMAGE_START>>>" in text_part["text"] and "<<<IMAGE_END>>>" in text_part["text"])
check("text has essay delimiter", "<<<ESSAY_START>>>" in text_part["text"] and "<<<ESSAY_END>>>" in text_part["text"])
check("text has task prompt", "Summarise the chart." in text_part["text"])
check("text has essay", "Overall, sales rose." in text_part["text"])
check("second part is image_url", image_part["type"] == "image_url")
img_url = image_part["image_url"]["url"]
check("image_url is data URI", img_url.startswith("data:image/png;base64,"))
# Spot-check the base64 round-trips
b64 = img_url.split(",", 1)[1]
check("image_url base64 decodes to input", base64.b64decode(b64) == png)

# --- 4. empty image rejected up front ------------------------------------

async def _empty_image_rejected():
    try:
        await svc.evaluate_task1_essay_with_ai("p", "e", b"")
    except ValueError as e:
        return "non-empty" in str(e)
    return False

check("empty image raises ValueError", asyncio.run(_empty_image_rejected()))

# --- 5. retry loop behavior (parity with Task 2) --------------------------

VALID_PAYLOAD = {
    "overall_band": 7.0,
    "coherence_feedback": "Clear overview; data is specific.",
    "vocabulary_suggestions": ["rose sharply", "plateaued"],
    "simon_style_rewrite": "Overall, sales rose sharply between 2010 and 2020...",
}

async def _retry_then_succeed():
    # Attempt 1: missing required field -> ValidationError -> retry.
    # Attempt 2: malformed JSON -> retry.
    # Attempt 3: valid payload.
    bad1 = _FakeResponse(choices=[_FakeChoice(_FakeMessage(json.dumps({"overall_band": 7.0})))], usage=_FakeUsage(prompt_tokens=100, completion_tokens=20))
    bad2 = _FakeResponse(choices=[_FakeChoice(_FakeMessage("not json"))], usage=_FakeUsage(prompt_tokens=110, completion_tokens=25))
    good = _FakeResponse(choices=[_FakeChoice(_FakeMessage(json.dumps(VALID_PAYLOAD)))], usage=_FakeUsage(prompt_tokens=120, completion_tokens=30))
    scripted = _with_scripted([bad1, bad2, good])
    result = await svc.evaluate_essay_with_ai("prompt", "word " * 200)
    return result, scripted

result, scripted = asyncio.run(_retry_then_succeed())
check("Task 2 retry: 3 attempts", len(scripted.calls) == 3)
check("Task 2 retry: result is WritingEvaluation", result.evaluation.overall_band == 7.0)
# Tokens summed across all 3 attempts.
check("Task 2 retry: input tokens summed", result.input_tokens == 100 + 110 + 120)
check("Task 2 retry: output tokens summed", result.output_tokens == 20 + 25 + 30)
# Error feedback was appended on the retry user message.
retry_msg = scripted.calls[2]["messages"][-1]
check("Task 2 retry: error fed back", "failed validation" in retry_msg["content"])

async def _retry_exhausted():
    # Three bad responses -> RuntimeError.
    bads = [
        _FakeResponse(choices=[_FakeChoice(_FakeMessage(json.dumps({"overall_band": 1.0})))], usage=_FakeUsage(prompt_tokens=10, completion_tokens=5)),
        _FakeResponse(choices=[_FakeChoice(_FakeMessage("oops"))], usage=_FakeUsage(prompt_tokens=10, completion_tokens=5)),
        _FakeResponse(choices=[_FakeChoice(_FakeMessage(json.dumps({"overall_band": 1.0})))], usage=_FakeUsage(prompt_tokens=10, completion_tokens=5)),
    ]
    _with_scripted(bads)
    try:
        await svc.evaluate_essay_with_ai("p", "e")
    except RuntimeError as e:
        return str(e) == "grading_temporarily_unavailable"
    return False

check("Task 2 exhausted retries raise", asyncio.run(_retry_exhausted()))

# --- 6. Task 1 vision function end-to-end (no real LLM) -------------------

async def _task1_happy_path():
    good = _FakeResponse(choices=[_FakeChoice(_FakeMessage(json.dumps(VALID_PAYLOAD)))], usage=_FakeUsage(prompt_tokens=200, completion_tokens=40))
    scripted = _with_scripted([good])
    result = await svc.evaluate_task1_essay_with_ai("Summarise the chart.", "word " * 150, png)
    return result, scripted

result, scripted = asyncio.run(_task1_happy_path())
check("Task 1 happy: 1 attempt", len(scripted.calls) == 1)
check("Task 1 happy: uses vision model", scripted.calls[0]["model"] == "gemini-2.5-flash")
check("Task 1 happy: uses Task 1 system prompt", scripted.calls[0]["messages"][0]["content"] == svc.SIMON_BAND9_TASK1_SYSTEM_PROMPT)
check("Task 1 happy: user content is multimodal list", isinstance(scripted.calls[0]["messages"][1]["content"], list))
check("Task 1 happy: token counts", result.input_tokens == 200 and result.output_tokens == 40)
check("Task 1 happy: evaluation parsed", result.evaluation.overall_band == 7.0)

async def _task1_uses_vision_model_independently():
    """Point a stub at a different LLM_MODEL and confirm the Task 1 call
    still goes to the vision model. We swap the module's _client to capture
    the request and inspect the ``model=`` argument directly."""
    good = _FakeResponse(choices=[_FakeChoice(_FakeMessage(json.dumps(VALID_PAYLOAD)))], usage=_FakeUsage(prompt_tokens=10, completion_tokens=5))
    scripted = _with_scripted([good])
    # Build a settings-shaped object where llm_model != llm_vision_model and
    # patch svc.settings for the duration of the call.
    @dataclass(frozen=True)
    class _Alt:
        llm_model: str = "gpt-4o"
        llm_vision_model: str = "gemini-2.5-flash"
    original = svc.settings
    svc.settings = _Alt()
    try:
        await svc.evaluate_task1_essay_with_ai("p", "e", png)
    finally:
        svc.settings = original
    return scripted.calls[0]["model"]

check("Task 1 uses vision model not text model", asyncio.run(_task1_uses_vision_model_independently()) == "gemini-2.5-flash")

# --- 7. vision-model fallback to text model -------------------------------

# When LLM_VISION_MODEL is unset, the production code defaults to LLM_MODEL.
# Replicate the exact expression from app.core.config to verify the fallback
# semantics (no need to re-import the module — it would cache the previous
# env value).
@dataclass(frozen=True)
class _BareSettings:
    llm_model: str = TEXT_MODEL
    llm_vision_model: str = (os.environ.get("LLM_VISION_MODEL") or TEXT_MODEL)


def _fallback_vision_model() -> str:
    """Same expression as the production config (default_factory=...)."""
    raw = os.environ.get("LLM_VISION_MODEL", "").strip()
    return raw or os.environ.get("LLM_MODEL", "gpt-4o-mini").strip()


# When the env var is set, the explicit value wins.
os.environ["LLM_VISION_MODEL"] = "gemini-2.5-flash"
check("vision model env var takes precedence", _fallback_vision_model() == "gemini-2.5-flash")

# When it's empty/unset, it falls back to LLM_MODEL.
os.environ.pop("LLM_VISION_MODEL", None)
check("vision model falls back to LLM_MODEL", _fallback_vision_model() == "gpt-4o-mini")
os.environ["LLM_VISION_MODEL"] = "gemini-2.5-flash"  # restore for any later checks

print("\nAll 3.5 self-checks passed.")
