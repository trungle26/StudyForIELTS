from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings
from app.core.database import close_mongo, connect_mongo
from app.routers import admin, feed, health, youtube


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_mongo(app)
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
    allow_methods=["GET"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(feed.router)
app.include_router(youtube.router)
app.include_router(admin.router)
