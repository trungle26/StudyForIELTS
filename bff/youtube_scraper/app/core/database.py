from fastapi import FastAPI, Request
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorCollection, AsyncIOMotorDatabase

from app.core.config import settings


async def connect_mongo(app: FastAPI) -> None:
    if not settings.mongodb_uri:
        raise RuntimeError("MONGODB_URI is required.")
    if "USER:PASSWORD@CLUSTER.mongodb.net" in settings.mongodb_uri or "cluster.mongodb.net" in settings.mongodb_uri:
        raise RuntimeError(
            "MONGODB_URI still contains the placeholder Atlas host. "
            "Replace it in bff/youtube_scraper/.env with your real MongoDB Atlas connection string."
        )

    client: AsyncIOMotorClient = AsyncIOMotorClient(
        settings.mongodb_uri,
        appname="studyforielts-youtube-bff",
        serverSelectionTimeoutMS=8000,
    )
    await client.admin.command("ping")

    app.state.mongo_client = client
    app.state.mongo_db = client[settings.mongodb_db_name]
    await app.state.mongo_db[settings.mongodb_collection].create_index("videoId", unique=True)
    await app.state.mongo_db[settings.mongodb_collection].create_index(
        [("level", 1), ("status", 1), ("curatedAt", -1)]
    )


async def close_mongo(app: FastAPI) -> None:
    client: AsyncIOMotorClient | None = getattr(app.state, "mongo_client", None)
    if client is not None:
        client.close()


def get_database(request: Request) -> AsyncIOMotorDatabase:
    return request.app.state.mongo_db


def get_curated_videos(request: Request) -> AsyncIOMotorCollection:
    return get_database(request)[settings.mongodb_collection]
