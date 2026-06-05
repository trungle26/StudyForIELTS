import { Router } from "express";
import { CEFR_LEVELS, CuratedVideo } from "../models/CuratedVideo.js";

export const feedRouter = Router();

feedRouter.get("/", async (req, res, next) => {
  try {
    const level = String(req.query.level || "").toUpperCase();

    if (!CEFR_LEVELS.includes(level)) {
      return res.status(400).json({ error: `level must be one of: ${CEFR_LEVELS.join(", ")}.` });
    }

    const defaultLimit = Number(process.env.FEED_PAGE_SIZE || 20);
    const maxLimit = Number(process.env.MAX_FEED_PAGE_SIZE || 50);
    const page = positiveInteger(req.query.page, 1);
    const limit = Math.min(positiveInteger(req.query.limit, defaultLimit), maxLimit);
    const skip = (page - 1) * limit;
    const query = { level, status: "published" };

    const [total, videos] = await Promise.all([
      CuratedVideo.countDocuments(query),
      CuratedVideo.find(query)
        .sort({ curatedAt: -1, createdAt: -1 })
        .skip(skip)
        .limit(limit)
        .select("-transcriptText -transcriptSegments -description")
        .lean()
    ]);

    return res.json({
      level,
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit),
      items: videos.map(toFeedItem)
    });
  } catch (error) {
    return next(error);
  }
});

feedRouter.get("/:videoId", async (req, res, next) => {
  try {
    const video = await CuratedVideo.findOne({
      videoId: req.params.videoId,
      status: "published"
    }).lean();

    if (!video) {
      return res.status(404).json({ error: "Video not found." });
    }

    return res.json({
      video: {
        ...toFeedItem(video),
        transcriptSegments: video.transcriptSegments
      }
    });
  } catch (error) {
    return next(error);
  }
});

function positiveInteger(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function toFeedItem(video) {
  return {
    id: video._id,
    videoId: video.videoId,
    title: video.title,
    channelTitle: video.channelTitle,
    thumbnailUrl: video.thumbnailUrl,
    durationSeconds: video.durationSeconds,
    publishDate: video.publishDate,
    level: video.level,
    computedLevel: video.computedLevel,
    confidence: video.classification?.confidence,
    tags: video.tags,
    curatedAt: video.curatedAt
  };
}
