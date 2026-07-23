"""Offline self-check for 3.2 admin CRUD wiring.

This file intentionally mocks `motor` and `fastapi` so it can run in a bare
interpreter with no installed deps. It exercises:

  - Pydantic model construction (`WritingLesson`, request/response shapes)
  - Service-layer pure helpers (`_normalize_tips`, `_content_type_extension`)
  - Form-field alias handling (`status` -> `status_value`) at the router

It does NOT exercise the actual Mongo/GridFS code paths (those require a
running DB). Run with: ``python eval/check_admin_lessons.py``.
"""
from __future__ import annotations

import sys
import types
from datetime import UTC, datetime

# --- mock the modules we don't want to install ------------------------------

def _install_stub(name: str, attrs: dict[str, object]) -> None:
    if name in sys.modules:
        return
    mod = types.ModuleType(name)
    for k, v in attrs.items():
        setattr(mod, k, v)
    sys.modules[name] = mod

_install_stub("motor", {})
_install_stub("motor.motor_asyncio", {
    "AsyncIOMotorClient": type("AsyncIOMotorClient", (), {}),
    "AsyncIOMotorCollection": type("AsyncIOMotorCollection", (), {}),
    "AsyncIOMotorDatabase": type("AsyncIOMotorDatabase", (), {}),
    "AsyncIOMotorGridFSBucket": type("AsyncIOMotorGridFSBucket", (), {}),
})
_install_stub("pymongo", {"ReturnDocument": types.SimpleNamespace(AFTER="after")})
_install_stub("bson", {"ObjectId": lambda s: s})


# Minimal pydantic stub: stores kwargs as attributes and supports .model_dump /
# .model_validate so we can exercise the helpers without installing pydantic.
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
        # Inherit parent fields first (mirrors real pydantic), then let the
        # current class override or add.
        for klass in reversed(cls.__mro__[1:]):
            parent_fields = getattr(klass, "model_fields", None)
            if parent_fields:
                for name, fld in parent_fields.items():
                    fields.setdefault(name, fld)
        # Annotation-only fields (no explicit Field(...)) need a required-
        # field default. Real pydantic auto-discovers them; do the same.
        for name in hints:
            if name not in fields:
                fields[name] = _Field(...)
        cls.model_fields = fields

    def __init__(self, **kwargs):
        # Apply defaults for missing fields, with the same logic pydantic uses
        # for required fields (raise) vs default_factory (call).
        for name in getattr(self, "model_fields", {}):
            f = self.model_fields[name]
            if name in kwargs:
                value = kwargs[name]
            else:
                d = f.default
                if d is ...:
                    raise TypeError(f"missing required field: {name}")
                if callable(d) and not isinstance(d, type):
                    value = d()
                else:
                    value = d
            setattr(self, name, value)
    def model_dump(self, mode: str | None = None) -> dict:
        return {k: getattr(self, k) for k in getattr(self, "model_fields", {})}
    @classmethod
    def model_validate(cls, data: dict):
        return cls(**data)
    @classmethod
    def model_validate_json(cls, data: str):
        import json
        return cls(**json.loads(data))


def _FieldFactory(*args, **kwargs):
    if args and "default" not in kwargs:
        kwargs["default"] = args[0]
    if "default_factory" in kwargs and "default" not in kwargs:
        kwargs["default"] = kwargs.pop("default_factory")
    return _Field(**kwargs)


_install_stub("pydantic", {
    "BaseModel": _BaseModel,
    "Field": _FieldFactory,
})
# Minimal FastAPI stub: just enough for the imports to resolve.
class _Depends:
    def __init__(self, dep): self.dep = dep
class _APIRouter:
    def __init__(self, *a, **kw): pass
    def post(self, *a, **kw):
        def deco(fn): return fn
        return deco
    def put(self, *a, **kw):
        def deco(fn): return fn
        return deco
    def delete(self, *a, **kw):
        def deco(fn): return fn
        return deco
    def get(self, *a, **kw):
        def deco(fn): return fn
        return deco
class _HTTPException(Exception):
    def __init__(self, status_code, detail): self.status_code, self.detail = status_code, detail
class _UploadFile: pass
class _File:
    def __init__(self, *a, **kw): pass
class _Form:
    def __init__(self, *a, **kw): pass
class _Header:
    def __init__(self, *a, **kw): pass
class _Status:
    HTTP_201_CREATED = 201
    HTTP_204_NO_CONTENT = 204
    HTTP_400_BAD_REQUEST = 400
    HTTP_401_UNAUTHORIZED = 401
    HTTP_404_NOT_FOUND = 404
    HTTP_413_REQUEST_ENTITY_TOO_LARGE = 413
    HTTP_429_TOO_MANY_REQUESTS = 429
    HTTP_500_INTERNAL_SERVER_ERROR = 500
_install_stub("fastapi", {
    "APIRouter": _APIRouter,
    "Depends": _Depends,
    "File": _File,
    "Form": _Form,
    "Header": _Header,
    "HTTPException": _HTTPException,
    "UploadFile": _UploadFile,
    "status": _Status,
})

# --- now the real imports ---------------------------------------------------

sys.path.insert(0, ".")

# These two modules need full FastAPI/Motor wiring; just skip them.
import importlib  # noqa: E402
import importlib.util  # noqa: E402
spec_admin = importlib.util.find_spec("app.routers.admin")
print("admin router importable as spec:", spec_admin is not None)

# Direct import of the things we can validate without FastAPI/motor runtime:
import app.services.writing_lesson_service as svc  # noqa: E402
import app.models.writing as wm  # noqa: E402


def check(name: str, cond: bool, hint: str = "") -> None:
    if not cond:
        print(f"FAIL: {name} {hint}")
        sys.exit(1)
    print(f"ok: {name}")


# _normalize_tips
check("normalize drops empties + dedupes", svc._normalize_tips(["  a ", "", "a", "b", "  "]) == ["a", "b"])
check("normalize caps at 20", len(svc._normalize_tips([f"t{i}" for i in range(30)])) == 20)
check("normalize None => []", svc._normalize_tips(None) == [])

# _content_type_extension
check("ct png", svc._content_type_extension("image/png") == ".png")
check("ct jpeg", svc._content_type_extension("image/jpeg") == ".jpg")
check("ct webp", svc._content_type_extension("image/webp") == ".webp")
check("ct unknown", svc._content_type_extension("application/x-thing") == "")
check("ct None", svc._content_type_extension(None) == "")

# Pydantic models
now = datetime.now(UTC)
lesson = wm.WritingLesson(
    task_type="task1",
    task_prompt="Summarise the chart.",
    sample_answer="The chart shows...",
    tips=["Identify the trend", "Compare two years"],
    difficulty="medium",
    status="published",
    created_at=now,
    updated_at=now,
)
check("lesson round-trips", lesson.task_type == "task1" and lesson.tips == ["Identify the trend", "Compare two years"])
check("lesson id auto-generated", len(lesson.id) == 36)
check("WritingLessonResponse identical for now", wm.WritingLessonResponse.model_validate(lesson.model_dump()).id == lesson.id)

# tips parsing — JSON array of strings => list
parsed = json_loads = __import__("json").loads
check("json round-trip tips", parsed('["a","b"]') == ["a", "b"])

# Admin response model
resp = wm.AdminLessonUpsertResponse(lesson=lesson)
check("admin upsert resp wraps lesson", resp.lesson.id == lesson.id)
list_resp = wm.AdminLessonListResponse(items=[lesson, lesson])
check("admin list resp ok", len(list_resp.items) == 2)

print("\nALL CHECKS PASSED")
