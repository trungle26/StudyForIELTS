from fastapi import APIRouter, Depends, status
from motor.motor_asyncio import AsyncIOMotorCollection

from app.core.database import get_curated_videos
from app.core.security import require_admin_token
from app.models.admin import AddVideoRequest, AddVideoResponse, AdminVideo
from app.services.admin_service import add_or_update_video


router = APIRouter(prefix="/admin", tags=["admin"])


@router.post(
    "/add-video",
    response_model=AddVideoResponse,
    status_code=status.HTTP_201_CREATED,
    dependencies=[Depends(require_admin_token)],
)
async def add_video(
    request: AddVideoRequest,
    collection: AsyncIOMotorCollection = Depends(get_curated_videos),
) -> AddVideoResponse:
    video = await add_or_update_video(collection, request)
    return AddVideoResponse(video=AdminVideo(**video))

