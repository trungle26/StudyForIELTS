from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.core.database import close_mongo, connect_mongo
from app.routers import admin, feed, health, writing, youtube
from app.routers.writing import WRITING_COLLECTION_NAME


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_mongo(app)
    # History queries sort by recency; create the index once at startup so
    # later list/history endpoints stay fast as the collection grows.
    writing_collection = app.state.mongo_db[WRITING_COLLECTION_NAME]
    await writing_collection.create_index([("created_at", -1)])
    try:
        yield
    finally:
        await close_mongo(app)


app = FastAPI(
    title="StudyForIELTS YouTube BFF",
    description="FastAPI BFF for curated IELTS listening videos, YouTube search, and transcripts.",
    version="2.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_allow_origins,
    allow_credentials=False,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(feed.router)
app.include_router(youtube.router)
app.include_router(admin.router)
app.include_router(writing.router)
