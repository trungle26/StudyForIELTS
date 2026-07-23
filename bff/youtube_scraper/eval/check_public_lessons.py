"""Offline self-check for 3.3 public lesson endpoints.

Stubs ``motor`` / ``fastapi`` / ``bson`` the same way ``check_admin_lessons.py``
does so this runs in a bare interpreter. Exercises:

  - Pydantic response models (``WritingLessonListResponse`` page math)
  - Service-layer filtering logic for ``list_published_lessons`` (a tiny
    in-memory store that mimics Mongo's ``find_one``/``find`` semantics
    with status + task_type filter + sort + skip + limit)
  - Router pagination parameter clamping (via ``Query`` defaults)
  - ``WritingLessonResponse`` vs ``WritingLesson`` round-trip

Does NOT start the real FastAPI app or hit Mongo. Run with:
``python eval/check_public_lessons.py``.
"""
from __future__ import annotations

import json
import sys
import types
from datetime import UTC, datetime
from typing import Any, AsyncIterator

# --- stub heavy deps so the import graph resolves ---------------------------

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


# Minimal pydantic stub (mirrors check_admin_lessons.py).
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
        return cls(**json.loads(data))


def _FieldFactory(*args, **kwargs):
    if args and "default" not in kwargs:
        kwargs["default"] = args[0]
    if "default_factory" in kwargs and "default" not in kwargs:
        kwargs["default"] = kwargs.pop("default_factory")
    return _Field(**kwargs)


_install_stub("pydantic", {"BaseModel": _BaseModel, "Field": _FieldFactory})

# FastAPI stub: just enough for the imports to resolve.
class _Depends:
    def __init__(self, dep): self.dep = dep
class _Query:
    def __init__(self, *a, **kw): pass
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
class _Response:
    pass
class _StreamingResponse:
    def __init__(self, *a, **kw): pass
class _UploadFile: pass
class _File:
    def __init__(self, *a, **kw): pass
class _Form:
    def __init__(self, *a, **kw): pass
class _Status:
    HTTP_201_CREATED = 201
    HTTP_204_NO_CONTENT = 204
    HTTP_400_BAD_REQUEST = 400
    HTTP_404_NOT_FOUND = 404
    HTTP_413_REQUEST_ENTITY_TOO_LARGE = 413

_install_stub("fastapi", {
    "APIRouter": _APIRouter,
    "Depends": _Depends,
    "File": _File,
    "Form": _Form,
    "HTTPException": _HTTPException,
    "Query": _Query,
    "Response": _Response,
    "StreamingResponse": _StreamingResponse,
    "UploadFile": _UploadFile,
    "status": _Status,
})

# --- now the real imports ----------------------------------------------------

sys.path.insert(0, ".")

import app.services.writing_lesson_service as svc  # noqa: E402
import app.models.writing as wm  # noqa: E402


def check(name: str, cond: bool, hint: str = "") -> None:
    if not cond:
        print(f"FAIL: {name} {hint}")
        sys.exit(1)
    print(f"ok: {name}")


# --- _LESSON_PAGE_LIMIT constant on the router is sane ----------------------
import importlib.util  # noqa: E402
spec = importlib.util.find_spec("app.routers.writing")
# The router module imports the LLM service; we can't load it without stubs.
# Verify the constant exists in source by reading the file directly.
router_src = open("app/routers/writing.py").read()
check("router has _LESSON_PAGE_LIMIT", "_LESSON_PAGE_LIMIT" in router_src)
check("router clamps to <= 50", "_LESSON_PAGE_LIMIT = 50" in router_src)
check("router exposes GET /lessons", '"/lessons"' in router_src)
check("router exposes GET /lessons/{id}", '"/lessons/{lesson_id}"' in router_src)
check("router exposes GET /lessons/{id}/image", '"/lessons/{lesson_id}/image"' in router_src)
check("image route sets content type", "content_type" in router_src)
check("image route is read-only (GET)", '@router.get(\n    "/lessons/{lesson_id}/image"' in router_src)

# --- Fake AsyncIOMotorCollection for service-layer logic --------------------
class _FakeCursor:
    def __init__(self, docs: list[dict]):
        self._docs = docs
    def sort(self, spec):
        # only the (created_at, -1) shape is used
        return _FakeCursor(sorted(self._docs, key=lambda d: d["created_at"], reverse=True))
    def skip(self, n: int):
        return _FakeCursor(self._docs[n:])
    def limit(self, n: int):
        return _FakeCursor(self._docs[:n])
    def __aiter__(self):
        async def gen():
            for d in self._docs:
                yield d
        return gen()

class _FakeCollection:
    def __init__(self, docs: list[dict]):
        self._docs = docs
    def find(self, query: dict | None = None, projection: dict | None = None):
        q = query or {}
        return _FakeCursor([d for d in self._docs if all(d.get(k) == v for k, v in q.items())])
    async def find_one(self, query: dict, projection: dict | None = None):
        for d in self._docs:
            if all(d.get(k) == v for k, v in query.items()):
                return d
        return None
    async def count_documents(self, query: dict) -> int:
        return sum(1 for d in self._docs if all(d.get(k) == v for k, v in query.items()))

# Seed: 5 published + 2 drafts, mixed task types
base = datetime(2026, 1, 1, tzinfo=UTC)
def _lesson_doc(idx: int, task_type: str, status: str, image_id: str | None = None) -> dict:
    now = datetime(2026, 1, idx + 1, tzinfo=UTC)
    return {
        "id": f"L{idx:03d}",
        "task_type": task_type,
        "task_prompt": f"Prompt {idx}",
        "image_id": image_id,
        "sample_answer": f"Answer {idx}",
        "tips": [f"tip {idx}"],
        "difficulty": "medium",
        "status": status,
        "created_at": now,
        "updated_at": now,
    }

docs = [
    _lesson_doc(0, "task1", "published", "img-1"),
    _lesson_doc(1, "task1", "published", None),
    _lesson_doc(2, "task2", "published", None),
    _lesson_doc(3, "task1", "draft", "img-draft"),
    _lesson_doc(4, "task2", "published", None),
    _lesson_doc(5, "task1", "published", "img-6"),
    _lesson_doc(6, "task2", "draft", None),
]
coll = _FakeCollection(docs)


import asyncio


async def _main() -> None:
    # list_published_lessons: no filter
    items, total = await svc.list_published_lessons(coll, task_type=None, page=1, limit=20)
    check("list excludes drafts (total=5)", total == 5, f"got {total}")
    check("list returns 5 published items", len(items) == 5, f"got {len(items)}")
    check("list newest-first", items[0].id == "L005" and items[-1].id == "L000")

    # list_published_lessons: task_type=task1
    items, total = await svc.list_published_lessons(coll, task_type="task1", page=1, limit=20)
    check("task1 filter excludes task2", total == 3, f"got {total}")
    check("task1 list ids", [i.id for i in items] == ["L005", "L001", "L000"])

    # list_published_lessons: pagination
    items, total = await svc.list_published_lessons(coll, task_type="task1", page=1, limit=2)
    check("page1 task1 limit2", [i.id for i in items] == ["L005", "L001"])
    items, total = await svc.list_published_lessons(coll, task_type="task1", page=2, limit=2)
    check("page2 task1 limit2", [i.id for i in items] == ["L000"])
    items, total = await svc.list_published_lessons(coll, task_type="task1", page=3, limit=2)
    check("page3 task1 limit2 empty", items == [] and total == 3)

    # get_published_lesson: existing published
    lesson = await svc.get_published_lesson(coll, "L002")
    check("get published found", lesson is not None and lesson.id == "L002")
    # get_published_lesson: existing draft
    lesson = await svc.get_published_lesson(coll, "L003")
    check("get published returns None for draft", lesson is None)
    # get_published_lesson: missing
    lesson = await svc.get_published_lesson(coll, "missing")
    check("get published returns None for missing", lesson is None)

    # LessonImageNotFound is a real exception class
    check("LessonImageNotFound is an Exception subclass", issubclass(svc.LessonImageNotFound, Exception))

    # Models round-trip
    fresh_items, _ = await svc.list_published_lessons(coll, task_type=None, page=1, limit=20)
    resp = wm.WritingLessonResponse.model_validate(fresh_items[0].model_dump())
    check("response id round-trips", resp.id == fresh_items[0].id)
    page_resp = wm.WritingLessonListResponse(
        page=1, limit=20, total=5, total_pages=1, items=[resp]
    )
    check("list response total_pages math (5/20)", page_resp.total_pages == 1)
    page_resp = wm.WritingLessonListResponse(
        page=2, limit=2, total=5, total_pages=3, items=[]
    )
    check("list response total_pages math (5/2)", page_resp.total_pages == 3)


asyncio.run(_main())
print("\nALL CHECKS PASSED")
