import { Router } from "express";
import { fetchTranscriptAndMetadata, searchYoutube, validateVideoId } from "../services/youtubeService.js";

export const youtubeRouter = Router();

youtubeRouter.get("/search", async (req, res, next) => {
  try {
    const query = String(req.query.q || "").trim();
    const defaultLimit = Number(process.env.DEFAULT_SEARCH_LIMIT || 10);
    const maxLimit = Number(process.env.MAX_SEARCH_LIMIT || 25);
    const limit = Math.min(positiveInteger(req.query.limit, defaultLimit), maxLimit);

    if (!query) {
      return res.status(400).json({ error: "q is required." });
    }

    const results = await searchYoutube(query, limit);
    return res.json({ query, results });
  } catch (error) {
    return next(error);
  }
});

youtubeRouter.get("/transcript", async (req, res, next) => {
  try {
    const videoId = String(req.query.videoId || "");
    const language = String(req.query.language || "en");

    if (!validateVideoId(videoId)) {
      return res.status(400).json({ error: "videoId must be an 11-character YouTube video ID." });
    }

    const video = await fetchTranscriptAndMetadata(videoId, language);
    return res.json({
      videoId: video.videoId,
      language: video.language,
      segments: video.transcriptSegments
    });
  } catch (error) {
    return next(error);
  }
});

function positiveInteger(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}
