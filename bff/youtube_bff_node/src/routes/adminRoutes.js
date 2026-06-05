import { Router } from "express";
import { CEFR_LEVELS, CuratedVideo } from "../models/CuratedVideo.js";
import { requireAdminToken } from "../middleware/adminAuth.js";
import { classifyCefr, countWords } from "../services/cefrClassifier.js";
import { fetchTranscriptAndMetadata, validateVideoId } from "../services/youtubeService.js";

export const adminRouter = Router();

adminRouter.post("/add-video", requireAdminToken, async (req, res, next) => {
  try {
    const {
      videoId,
      language = "en",
      levelOverride,
      tags = [],
      status = "published"
    } = req.body || {};

    if (!validateVideoId(videoId || "")) {
      return res.status(400).json({ error: "videoId must be an 11-character YouTube video ID." });
    }

    if (levelOverride && !CEFR_LEVELS.includes(levelOverride)) {
      return res.status(400).json({ error: `levelOverride must be one of: ${CEFR_LEVELS.join(", ")}.` });
    }

    if (!["published", "draft"].includes(status)) {
      return res.status(400).json({ error: "status must be either published or draft." });
    }

    const youtubeData = await fetchTranscriptAndMetadata(videoId, language);
    const classification = classifyCefr(youtubeData.transcriptText);
    const computedLevel = classification.level;
    const finalLevel = levelOverride || computedLevel;

    const curatedVideo = await CuratedVideo.findOneAndUpdate(
      { videoId },
      {
        ...youtubeData,
        wordCount: countWords(youtubeData.transcriptText),
        level: finalLevel,
        computedLevel,
        levelSource: levelOverride ? "manual" : "computed",
        classification,
        tags: normalizeTags(tags),
        status,
        curatedAt: new Date()
      },
      {
        new: true,
        upsert: true,
        runValidators: true,
        setDefaultsOnInsert: true
      }
    ).lean();

    return res.status(201).json({
      video: toAdminVideoResponse(curatedVideo)
    });
  } catch (error) {
    return next(error);
  }
});

function normalizeTags(tags) {
  if (!Array.isArray(tags)) {
    return [];
  }

  return [...new Set(tags.map((tag) => String(tag).trim().toLowerCase()).filter(Boolean))].slice(0, 20);
}

function toAdminVideoResponse(video) {
  return {
    id: video._id,
    videoId: video.videoId,
    title: video.title,
    channelTitle: video.channelTitle,
    thumbnailUrl: video.thumbnailUrl,
    durationSeconds: video.durationSeconds,
    language: video.language,
    wordCount: video.wordCount,
    level: video.level,
    computedLevel: video.computedLevel,
    levelSource: video.levelSource,
    classification: video.classification,
    tags: video.tags,
    status: video.status,
    transcriptSegmentCount: video.transcriptSegments?.length || 0,
    curatedAt: video.curatedAt
  };
}
