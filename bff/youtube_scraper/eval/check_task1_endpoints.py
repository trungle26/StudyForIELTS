"""Offline self-check for 3.6 — Task 1 evaluate endpoints.

This file mocks the modules we don't want to install (motor/fastapi/openai)
and exercises the new code paths added in Priority 3.6:

  - `Task1EssaySubmission` model has the right fields.
  - `_fingerprint` includes the task-type prefix so Task 1 / Task 2 essays
    with the same text never collide in the response cache.
  - `evaluate_task1_essay_with_ai_stream` rejects empty image bytes up
    front with an `event: error` (instead of crashing the stream), uses the
    Task 1 system prompt, and routes through the vision model.
  - `evaluate_task1_essay_with_ai_stream` shares its event protocol with
    `stream_essay_evaluation` (raw `data:` deltas, one `event: usage`,
    one `event: done`).
  - `WritingEvaluationDB.task_type` defaults to "task2" so older documents
    still deserialize, and accepts "task1" for new writes.
  - The two new endpoints are registered on the writing router:
    `POST /writing/evaluate/task1` and `POST /writing/evaluate/task1/stream`.

Run with: ``python eval/check_task1_endpoints.py``.
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

# Tiny pydantic stub: enough for Task1EssaySubmission / WritingEvaluationDB /
# Field / model_validate / model_dump / model_dump_json. Same shape as the
# stub in check_task1_llm_service.py.

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

# pydantic.dataclasses.dataclass is imported by app.core.config. We don't
# load it here because we swap the entire settings module below; just stub
# it in case anything else imports from it.
_install_stub("pydantic.dataclasses", {"dataclass": lambda *a, **kw: (lambda c: c)})

class _ValidationError(TypeError):
    pass

pydantic_stub = sys.modules["pydantic"]
pydantic_stub.ValidationError = _ValidationError

# --- minimal motor / bson / fastapi / openai stubs ------------------------

# `bson` is a real package (pymongo). We don't stub it; the real package
# provides the ObjectId we need.

# Motor: a single AsyncIOMotorGridFSBucket placeholder; we don't call it.
class _AsyncIOMotorGridFSBucket:
    def __init__(self, *a, **kw):
        self.kw = kw

class _AsyncIOMotorCollection:
    def __init__(self):
        self.calls: list[tuple[str, dict]] = []

class _AsyncIOMotorClient:
    def __init__(self, *a, **kw):
        self.kw = kw
    def __getitem__(self, name):
        return types.SimpleNamespace(
            __getattr__=_missing,
        )

def _missing(*a, **kw):
    raise NotImplementedError("not used in this self-check")

class _AsyncIOMotorDatabase:
    def __init__(self):
        self._cols: dict[str, _AsyncIOMotorCollection] = {}
    def __getitem__(self, name: str) -> _AsyncIOMotorCollection:
        if name not in self._cols:
            self._cols[name] = _AsyncIOMotorCollection()
        return self._cols[name]

class _AsyncIOMotorClientFull:
    def __init__(self, *a, **kw):
        self._db = _AsyncIOMotorDatabase()
    def __getitem__(self, name: str):
        return self._db
    @property
    def admin(self):
        return types.SimpleNamespace(command=lambda *a, **kw: asyncio.sleep(0))

_install_stub(
    "motor.motor_asyncio",
    {
        "AsyncIOMotorClient": _AsyncIOMotorClientFull,
        "AsyncIOMotorCollection": _AsyncIOMotorCollection,
        "AsyncIOMotorDatabase": _AsyncIOMotorDatabase,
        "AsyncIOMotorGridFSBucket": _AsyncIOMotorGridFSBucket,
    },
)

# FastAPI: enough surface for our imports and Depends() with None.
class _Depends:
    def __init__(self, dep):
        self.dep = dep

class _HTTPException(Exception):
    def __init__(self, status_code: int, detail: Any = None):
        self.status_code = status_code
        self.detail = detail
        super().__init__(f"{status_code}: {detail}")

class _Request:
    pass

class _Query:
    def __init__(self, *a, **kw):
        pass

class _Response:
    pass

class _StreamingResponse:
    def __init__(self, *a, **kw):
        pass

class _APIRouter:
    def __init__(self, *a, **kw):
        self.routes: list[Any] = []
        self.prefix = kw.get("prefix", "")
    def post(self, path, **kw):
        def decorator(fn):
            self.routes.append(("POST", self.prefix + path, fn, kw))
            return fn
        return decorator
    def get(self, path, **kw):
        def decorator(fn):
            self.routes.append(("GET", self.prefix + path, fn, kw))
            return fn
        return decorator

class _FastAPI:
    def __init__(self, *a, **kw):
        self.state = types.SimpleNamespace()

class _Status:
    HTTP_429_TOO_MANY_REQUESTS = 429

_install_stub(
    "fastapi",
    {
        "APIRouter": _APIRouter,
        "Depends": _Depends,
        "FastAPI": _FastAPI,
        "HTTPException": _HTTPException,
        "Query": _Query,
        "Request": _Request,
        "status": _Status,
    },
)
_install_stub(
    "fastapi.responses",
    {"Response": _Response, "StreamingResponse": _StreamingResponse},
)

# OpenAI: minimal surface used by llm_service.
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
    def __init__(self, scripts):
        # ``scripts`` is a list of either: an AsyncIter of chunks
        # (streaming call) or a single response (non-streaming call).
        # Each ``create()`` consumes one entry.
        self._scripts = list(scripts)
        self.calls: list[dict[str, Any]] = []

    async def create(self, **kwargs):
        self.calls.append(kwargs)
        if not self._scripts:
            raise AssertionError("scripted responses exhausted")
        next_response = self._scripts.pop(0)
        stream = kwargs.get("stream")
        if stream:
            # Wrap whatever was scripted (a single chunk or a list of
            # chunks) into an async iterator the model code can drain.
            if isinstance(next_response, _AsyncIter):
                return next_response
            if isinstance(next_response, list):
                return _AsyncIter(next_response)
            return _AsyncIter([next_response])
        return next_response


class _AsyncIter:
    def __init__(self, items):
        self._items = list(items)
    def __aiter__(self):
        self._iter = iter(self._items)
        return self
    async def __anext__(self):
        try:
            return next(self._iter)
        except StopIteration as e:
            raise StopAsyncIteration from e

class _FakeAsyncOpenAI:
    def __init__(self, scripted):
        self.chat = types.SimpleNamespace(completions=_FakeChatCompletions(scripted))

class _AsyncOpenAI:
    def __init__(self, *a, **kw):
        raise RuntimeError("placeholder")

class _ChatCompletionMessageParam(dict):
    pass

_install_stub("openai", {"AsyncOpenAI": _AsyncOpenAI})
_install_stub("openai.types", {})
_install_stub("openai.types.chat", {"ChatCompletionMessageParam": _ChatCompletionMessageParam})

# --- settings stub --------------------------------------------------------

import os  # noqa: E402
os.environ.setdefault("LLM_API_KEY", "sk-test")
os.environ.setdefault("MONGODB_URI", "mongodb://localhost:27017")
os.environ["LLM_MODEL"] = "gpt-4o-mini"
os.environ["LLM_VISION_MODEL"] = "gemini-2.5-flash"
TEXT_MODEL = os.environ["LLM_MODEL"]
VISION_MODEL = os.environ["LLM_VISION_MODEL"]

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

_STUB_SETTINGS = _Settings()
sys.path.insert(0, ".")

import app.core.config as _config_module  # noqa: E402
_config_module.settings = _STUB_SETTINGS
_config_module.get_settings = lambda: _STUB_SETTINGS

# Now load the modules under test.
import app.models.writing as models  # noqa: E402
import app.services.llm_service as svc  # noqa: E402
svc.settings = _STUB_SETTINGS
import app.routers.writing as rtr  # noqa: E402

# --- helpers ---------------------------------------------------------------

def check(name: str, cond: bool, hint: str = "") -> None:
    if not cond:
        print(f"FAIL: {name} {hint}")
        sys.exit(1)
    print(f"ok: {name}")

def _with_scripted(scripts) -> _FakeChatCompletions:
    """Script the chat completions client with one entry per ``create()`` call.

    Each entry is either a single ``_FakeResponse`` (non-streaming call) or
    a list of ``_FakeResponse`` chunks (streaming call, drained as a single
    async iterator).
    """
    fake = _FakeAsyncOpenAI(scripts)
    svc._client = fake  # type: ignore[assignment]
    return fake.chat.completions

# Tiny PNG signature for image-related checks.
PNG = b"\x89PNG\r\n\x1a\n" + b"\x00" * 16

# --- 1. Task1EssaySubmission model shape ---------------------------------

required = set(models.Task1EssaySubmission.model_fields.keys())
check("Task1EssaySubmission has lesson_id", "lesson_id" in required)
check("Task1EssaySubmission has essay_text", "essay_text" in required)
check("Task1EssaySubmission has no task_prompt", "task_prompt" not in required)
check(
    "Task1EssaySubmission requires both fields",
    "lesson_id" in required and "essay_text" in required and len(required) == 2,
)

# Build a valid instance and check the field round-trips.
submission = models.Task1EssaySubmission(lesson_id="abc-123", essay_text="My essay.")
check("Task1EssaySubmission.lesson_id roundtrip", submission.lesson_id == "abc-123")
check("Task1EssaySubmission.essay_text roundtrip", submission.essay_text == "My essay.")

# Missing lesson_id should fail.
try:
    models.Task1EssaySubmission(essay_text="x")  # type: ignore[call-arg]
    missing_lesson = True
except _ValidationError:
    missing_lesson = False
check("Task1EssaySubmission rejects missing lesson_id", not missing_lesson)

# --- 2. WritingEvaluationDB gains task_type with task2 default ------------

fields = set(models.WritingEvaluationDB.model_fields.keys())
check("WritingEvaluationDB has task_type field", "task_type" in fields)
# Construct one with the minimum required fields; the model_dump() should
# report task_type == "task2" by default.
ev = models.WritingEvaluationDB(
    id="x",
    task_prompt="p",
    essay_text="e",
    created_at="2026-01-01T00:00:00Z",
    overall_band=7.0,
    coherence_feedback="c",
    vocabulary_suggestions=["a"],
    simon_style_rewrite="r",
)
check("WritingEvaluationDB defaults task_type=task2", ev.task_type == "task2")

# Setting task_type=task1 should work.
ev1 = models.WritingEvaluationDB(
    id="y",
    task_prompt="p",
    essay_text="e",
    created_at="2026-01-01T00:00:00Z",
    task_type="task1",
    overall_band=7.0,
    coherence_feedback="c",
    vocabulary_suggestions=["a"],
    simon_style_rewrite="r",
)
check("WritingEvaluationDB accepts task_type=task1", ev1.task_type == "task1")

# --- 3. _fingerprint distinguishes Task 1 from Task 2 -------------------

fp_t2_a = rtr._fingerprint("v2", "prompt", "essay text")
fp_t2_b = rtr._fingerprint("v2", "prompt", "essay text")  # identical inputs
check("Task 2 fingerprint is deterministic", fp_t2_a == fp_t2_b)

fp_t1_a = rtr._fingerprint("v1", "prompt", "essay text", task_type="task1")
fp_t1_b = rtr._fingerprint("v1", "prompt", "essay text", task_type="task1")
check("Task 1 fingerprint is deterministic", fp_t1_a == fp_t1_b)

check("Task 1 and Task 2 fingerprints never collide", fp_t1_a != fp_t2_a)
# Different prompt version still produces a different fingerprint.
fp_t1_other = rtr._fingerprint("v2", "prompt", "essay text", task_type="task1")
check("Fingerprint depends on prompt version", fp_t1_a != fp_t1_other)
# Whitespace-only differences still produce a stable hash (strip semantics).
fp_t1_ws = rtr._fingerprint("v1", "  prompt  ", "\n essay text \n", task_type="task1")
check("Fingerprint strips whitespace", fp_t1_a == fp_t1_ws)

# --- 4. evaluate_task1_essay_with__stream exists and uses vision model --

assert callable(getattr(svc, "evaluate_task1_essay_with_ai_stream", None)), \
    "evaluate_task1_essay_with_ai_stream must exist on llm_service"

VALID_PAYLOAD = {
    "overall_band": 7.5,
    "coherence_feedback": "Clear overview; data referenced accurately.",
    "vocabulary_suggestions": ["rose sharply", "plateaued", "fluctuated"],
    "simon_style_rewrite": "Overall, sales rose sharply between 2010 and 2020...",
}

async def _stream_happy_path():
    # For a streaming call, the OpenAI SDK yields chunks; one chunk per
    # delta in production. Our model code does
    #   async for chunk in stream:
    #     delta = chunk.choices[0].delta.content
    # so each chunk needs ``choices`` with a ``delta`` carrying the content.
    # Real OpenAI streams also emit a final chunk with no ``choices`` but
    # with ``usage`` (when ``stream_options={"include_usage": True}``); the
    # model code looks for that branch to capture token counts, so we
    # include it here.
    content = json.dumps(VALID_PAYLOAD)
    content_chunk = _FakeResponse(
        choices=[_FakeChoice(_FakeMessage(content))],
        # No usage on the content chunk; usage arrives on the final chunk.
    )
    usage_chunk = _FakeResponse(
        choices=[],
        usage=_FakeUsage(prompt_tokens=300, completion_tokens=60),
    )
    # Make the content chunk's choice readable as both .delta.content
    # (streaming) and .message.content (non-streaming retry fallback).
    content_chunk.choices[0] = _StreamingChoice(content=content)
    # One streaming call -> a list of two chunks.
    stream_chunks = [content_chunk, usage_chunk]
    scripted = _with_scripted([stream_chunks])
    events = []
    async for ev in svc.evaluate_task1_essay_with_ai_stream(
        task_prompt="Summarise the chart.", essay_text="word " * 150, image_bytes=PNG
    ):
        events.append(ev)
    return events, scripted


@dataclass
class _StreamingChoice:
    content: str = ""

    @property
    def delta(self) -> "_StreamingDelta":
        return _StreamingDelta(content=self.content)

    @property
    def message(self) -> _FakeMessage:
        # The retry path reads .message.content; reuse the streaming content.
        return _FakeMessage(content=self.content)


@dataclass
class _StreamingDelta:
    content: str = ""

events, scripted = asyncio.run(_stream_happy_path())
# 1 attempt (no retry needed).
check("Task 1 stream: 1 attempt on happy path", len(scripted.calls) == 1)
check("Task 1 stream: uses vision model", scripted.calls[0]["model"] == VISION_MODEL)
check(
    "Task 1 stream: uses Task 1 system prompt",
    scripted.calls[0]["messages"][0]["content"] == svc.SIMON_BAND9_TASK1_SYSTEM_PROMPT,
)
check("Task 1 stream: user content is multimodal", isinstance(scripted.calls[0]["messages"][1]["content"], list))
check("Task 1 stream: 2 content parts (text + image_url)", len(scripted.calls[0]["messages"][1]["content"]) == 2)

# Event protocol parity: raw data deltas, one usage event, one done event.
text_deltas = [e for e in events if e.startswith("data: ") and not e.startswith("event: ")]
usage_events = [e for e in events if e.startswith("event: usage")]
done_events = [e for e in events if e.startswith("event: done")]
error_events = [e for e in events if e.startswith("event: error")]
check("Task 1 stream: at least one raw data: delta", len(text_deltas) >= 1)
check("Task 1 stream: exactly one usage event", len(usage_events) == 1)
check("Task 1 stream: exactly one done event", len(done_events) == 1)
check("Task 1 stream: no error event on happy path", len(error_events) == 0)

# The usage event payload should be valid JSON with the token counts.
usage_payload = json.loads(usage_events[0].split("data: ", 1)[1])
check("Task 1 stream: usage reports input_tokens", usage_payload["input_tokens"] == 300)
check("Task 1 stream: usage reports output_tokens", usage_payload["output_tokens"] == 60)

# The done event payload should round-trip as a WritingEvaluation.
done_payload = done_events[0].split("data: ", 1)[1]
parsed = models.WritingEvaluation.model_validate_json(done_payload)
check("Task 1 stream: done event is a valid WritingEvaluation", parsed.overall_band == 7.5)

# --- 5. Empty image guard -----------------------------------------------

async def _stream_empty_image():
    # Use a fresh client so we can verify the empty-image guard short-
    # circuits before any LLM call is made.
    scripted = _with_scripted([_FakeResponse(choices=[])])  # one dummy, must NOT be consumed
    events = []
    async for ev in svc.evaluate_task1_essay_with_ai_stream(
        task_prompt="p", essay_text="word " * 150, image_bytes=b""
    ):
        events.append(ev)
    return events, scripted

empty_events, empty_scripted = asyncio.run(_stream_empty_image())
check("Task 1 stream: empty image emits error event", any(e.startswith("event: error") for e in empty_events))
# The error message should be specific so the Android client can branch on it.
err_event = next(e for e in empty_events if e.startswith("event: error"))
check(
    "Task 1 stream: empty image error mentions image",
    "image" in err_event.lower(),
)
# The empty-image path must NOT call the LLM at all.
check(
    "Task 1 stream: empty image does not call LLM",
    len(empty_scripted.calls) == 0,
)

# --- 6. Routers register the new endpoints ------------------------------

# Collect every route declared on the writing router.
paths = [(method, path) for method, path, _fn, _kw in rtr.router.routes]
check("POST /writing/evaluate is registered", ("POST", "/writing/evaluate") in paths)
check("POST /writing/evaluate/stream is registered", ("POST", "/writing/evaluate/stream") in paths)
check("POST /writing/evaluate/task1 is registered", ("POST", "/writing/evaluate/task1") in paths)
check("POST /writing/evaluate/task1/stream is registered", ("POST", "/writing/evaluate/task1/stream") in paths)
# Task 2 endpoints must still be present (backward-compat).
check("GET /writing/lessons still registered", ("GET", "/writing/lessons") in paths)
check("GET /writing/lessons/{id} still registered", ("GET", "/writing/lessons/{lesson_id}") in paths)

# Each new endpoint has the rate-limit dependency wired in.
def _has_rate_limit(method: str, path: str) -> bool:
    for m, p, _fn, kw in rtr.router.routes:
        if m == method and p == path:
            deps = kw.get("dependencies", [])
            return any(
                getattr(d, "dep", None) is rtr.check_rate_limit for d in deps
            )
    return False

check("POST /writing/evaluate/task1 has rate-limit", _has_rate_limit("POST", "/writing/evaluate/task1"))
check("POST /writing/evaluate/task1/stream has rate-limit", _has_rate_limit("POST", "/writing/evaluate/task1/stream"))

print("\nAll 3.6 self-checks passed.")
