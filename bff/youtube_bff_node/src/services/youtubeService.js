import TranscriptClient from "youtube-transcript-api";
import { fetchTranscript as fetchFallbackTranscript } from "youtube-transcript";

const VIDEO_ID_PATTERN = /^[a-zA-Z0-9_-]{11}$/;
const DEFAULT_LANGUAGE = "en";
const requestTimeoutMs = Number(process.env.YOUTUBE_REQUEST_TIMEOUT_MS || 15000);

let transcriptClientPromise;

export function validateVideoId(videoId) {
  return VIDEO_ID_PATTERN.test(videoId);
}

export async function searchYoutube(query, limit) {
  const url = new URL("https://www.youtube.com/results");
  url.searchParams.set("search_query", query);
  url.searchParams.set("sp", "EgIQAQ==");

  const response = await fetch(url, {
    signal: AbortSignal.timeout(requestTimeoutMs),
    headers: {
      "Accept-Language": "en-US,en;q=0.9",
      "User-Agent":
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
    }
  });

  if (!response.ok) {
    const error = new Error(`YouTube search failed with status ${response.status}.`);
    error.status = 502;
    throw error;
  }

  const html = await response.text();
  const initialData = parseYtInitialData(html);
  const renderers = [];
  collectVideoRenderers(initialData, renderers);

  return renderers.slice(0, limit).map((renderer) => ({
    videoId: renderer.videoId,
    title: renderer.title?.runs?.[0]?.text || renderer.title?.simpleText || "",
    thumbnails: (renderer.thumbnail?.thumbnails || []).map((thumbnail) => ({
      url: thumbnail.url,
      width: thumbnail.width || null,
      height: thumbnail.height || null
    }))
  }));
}

export async function fetchTranscriptAndMetadata(videoId, language = DEFAULT_LANGUAGE) {
  if (!validateVideoId(videoId)) {
    const error = new Error("videoId must be an 11-character YouTube video ID.");
    error.status = 400;
    throw error;
  }

  try {
    return await fetchTranscriptFromTranscriptApi(videoId, language);
  } catch (primaryError) {
    return fetchTranscriptFromFallbackProvider(videoId, language, primaryError);
  }
}

async function fetchTranscriptFromTranscriptApi(videoId, language) {
  const client = await getTranscriptClient();
  const data = await client.getTranscript(videoId, { timeout: requestTimeoutMs });
  const track = selectTranscriptTrack(data.tracks || [], language);

  if (!track?.transcript?.length) {
    const reason = data.playabilityStatus?.reason || "No transcript is available for this video.";
    const error = new Error(reason);
    error.status = 404;
    throw error;
  }

  const transcriptSegments = track.transcript
    .map((segment) => {
      const startTime = Number(segment.start);
      const duration = Number(segment.dur || 0);

      return {
        startTime: round(startTime),
        endTime: round(startTime + duration),
        text: cleanText(segment.text)
      };
    })
    .filter((segment) => Number.isFinite(segment.startTime) && segment.text);

  const transcriptText = transcriptSegments.map((segment) => segment.text).join(" ");
  const metadata = normalizeMetadata(data);

  return {
    ...metadata,
    videoId,
    language: track.language || language,
    transcriptSegments,
    transcriptText
  };
}

async function fetchTranscriptFromFallbackProvider(videoId, language, primaryError) {
  try {
    const [metadata, transcript] = await Promise.all([
      fetchOembedMetadata(videoId),
      fetchFallbackTranscript(videoId, {
        lang: language,
        fetch: fetchWithTimeout
      })
    ]);

    const transcriptSegments = transcript
      .map((segment) => {
        const startTime = Number(segment.offset) / 1000;
        const duration = Number(segment.duration || 0) / 1000;

        return {
          startTime: round(startTime),
          endTime: round(startTime + duration),
          text: cleanText(segment.text)
        };
      })
      .filter((segment) => Number.isFinite(segment.startTime) && segment.text);

    if (!transcriptSegments.length) {
      const error = new Error("No transcript is available for this video.");
      error.status = 404;
      throw error;
    }

    return {
      ...metadata,
      videoId,
      language: transcript[0]?.lang || language,
      transcriptSegments,
      transcriptText: transcriptSegments.map((segment) => segment.text).join(" ")
    };
  } catch (fallbackError) {
    const error = new Error(`Transcript fetch failed: ${fallbackError.message || primaryError.message}`);
    error.status = fallbackError.status || statusFromTranscriptError(fallbackError);
    error.cause = fallbackError;
    throw error;
  }
}

async function getTranscriptClient() {
  if (!transcriptClientPromise) {
    const client = new TranscriptClient({
      timeout: requestTimeoutMs,
      headers: {
        "User-Agent":
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
      }
    });

    transcriptClientPromise = client.ready.then(() => client);
  }

  return transcriptClientPromise;
}

function selectTranscriptTrack(tracks, language) {
  const normalizedLanguage = language.toLowerCase();

  return (
    tracks.find((track) => track.language?.toLowerCase() === normalizedLanguage) ||
    tracks.find((track) => track.language?.toLowerCase().startsWith(normalizedLanguage)) ||
    tracks.find((track) => track.language?.toLowerCase().includes("english")) ||
    tracks[0]
  );
}

function normalizeMetadata(data) {
  const microformat = data.microformat?.playerMicroformatRenderer || {};
  const title = data.title || microformat.title?.simpleText || "";
  const thumbnails = microformat.thumbnail?.thumbnails || [];
  const thumbnail = thumbnails.at(-1)?.url || `https://i.ytimg.com/vi/${data.id}/hqdefault.jpg`;
  const durationSeconds = Number(microformat.lengthSeconds);
  const publishDate = microformat.publishDate || microformat.uploadDate;

  return {
    title,
    description: microformat.description?.simpleText || "",
    channelId: data.channelId || microformat.externalChannelId || "",
    channelTitle: data.author || microformat.ownerChannelName || "",
    publishDate: publishDate ? new Date(publishDate) : undefined,
    durationSeconds: Number.isFinite(durationSeconds) ? durationSeconds : undefined,
    thumbnailUrl: thumbnail,
    embedUrl: microformat.embed?.iframeUrl || `https://www.youtube.com/embed/${data.id}`
  };
}

async function fetchOembedMetadata(videoId) {
  const url = new URL("https://www.youtube.com/oembed");
  url.searchParams.set("url", `https://www.youtube.com/watch?v=${videoId}`);
  url.searchParams.set("format", "json");

  const response = await fetch(url, {
    signal: AbortSignal.timeout(requestTimeoutMs),
    headers: {
      "Accept-Language": "en-US,en;q=0.9",
      "User-Agent":
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
    }
  });

  if (!response.ok) {
    const error = new Error(`YouTube metadata fetch failed with status ${response.status}.`);
    error.status = response.status === 404 ? 404 : 502;
    throw error;
  }

  const data = await response.json();

  return {
    title: data.title || `YouTube video ${videoId}`,
    description: "",
    channelId: "",
    channelTitle: data.author_name || "",
    publishDate: undefined,
    durationSeconds: undefined,
    thumbnailUrl: data.thumbnail_url || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
    embedUrl: `https://www.youtube.com/embed/${videoId}`
  };
}

function fetchWithTimeout(input, init = {}) {
  return fetch(input, {
    ...init,
    signal: init.signal || AbortSignal.timeout(requestTimeoutMs),
    headers: {
      "Accept-Language": "en-US,en;q=0.9",
      "User-Agent":
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
      ...(init.headers || {})
    }
  });
}

function statusFromTranscriptError(error) {
  const message = String(error.message || "").toLowerCase();

  if (message.includes("too many")) {
    return 429;
  }

  if (
    message.includes("unavailable") ||
    message.includes("disabled") ||
    message.includes("not available") ||
    message.includes("not found")
  ) {
    return 404;
  }

  return 502;
}

function cleanText(text) {
  return String(text)
    .replace(/<[^>]*>/g, "")
    .replace(/\[[^\]]*]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

function round(value) {
  return Number(value.toFixed(3));
}

function parseYtInitialData(html) {
  const marker = "ytInitialData";
  const markerIndex = html.indexOf(marker);

  if (markerIndex === -1) {
    const error = new Error("Could not find YouTube initial data in search response.");
    error.status = 502;
    throw error;
  }

  const firstBrace = html.indexOf("{", markerIndex);
  if (firstBrace === -1) {
    const error = new Error("Could not parse YouTube search response.");
    error.status = 502;
    throw error;
  }

  let depth = 0;
  let inString = false;
  let isEscaped = false;

  for (let index = firstBrace; index < html.length; index += 1) {
    const char = html[index];

    if (inString) {
      if (isEscaped) {
        isEscaped = false;
      } else if (char === "\\") {
        isEscaped = true;
      } else if (char === "\"") {
        inString = false;
      }
      continue;
    }

    if (char === "\"") {
      inString = true;
    } else if (char === "{") {
      depth += 1;
    } else if (char === "}") {
      depth -= 1;

      if (depth === 0) {
        return JSON.parse(html.slice(firstBrace, index + 1));
      }
    }
  }

  const error = new Error("YouTube search response ended before initial data was complete.");
  error.status = 502;
  throw error;
}

function collectVideoRenderers(value, renderers) {
  if (!value || typeof value !== "object") {
    return;
  }

  if (value.videoRenderer?.videoId) {
    renderers.push(value.videoRenderer);
    return;
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      collectVideoRenderers(item, renderers);
    }
    return;
  }

  for (const child of Object.values(value)) {
    collectVideoRenderers(child, renderers);
  }
}
