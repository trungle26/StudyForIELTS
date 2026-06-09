const path = require("node:path");
const dotenv = require("dotenv");

dotenv.config({
    path: path.resolve(__dirname, "..", ".env")
});

// const API_URL = process.env.SEED_API_URL || `http://127.0.0.1:${process.env.PORT || 8000}`;
const API_URL = "https://studyforielts-youtube-bff.onrender.com";
const ADMIN_TOKEN = process.env.ADMIN_TOKEN;
const REQUEST_TIMEOUT_MS = Number(process.env.SEED_REQUEST_TIMEOUT_MS || 90000);

const defaultVideoIds = [
    "dQw4w9WgXcQ",
    "sVpiPQcNGg4",
    "5QdrP-EBOK4",
    "Q8G8hbmDSLg",
    "mkLGScX9Tl0",
    "vOXFKYGkPvI",
    "lXLBTBBil2U", "Xn1EsFe7snQ"
];

const videoIds = (process.env.SEED_VIDEO_IDS || defaultVideoIds.join(","))
    .split(",")
    .map((videoId) => videoId.trim())
    .filter(Boolean);

async function seedDatabase() {
    if (!ADMIN_TOKEN) {
        throw new Error("ADMIN_TOKEN is missing. Set it in ../.env before running the seed script.");
    }

    console.log("Starting curated video seed...");
    console.log(`API: ${API_URL}`);

    await assertServerIsRunning();

    for (const videoId of videoIds) {
        await addVideoWithRetry(videoId, 2);
    }

    console.log("Seed complete.");
}

async function assertServerIsRunning() {
    try {
        const response = await fetch(`${API_URL}/health`, {
            signal: AbortSignal.timeout(5000)
        });

        if (!response.ok) {
            throw new Error(`Health check returned HTTP ${response.status}.`);
        }
    } catch (error) {
        throw new Error(
            `Cannot reach ${API_URL}/health. Start the server with "npm run dev" from bff/youtube_bff_node first. ${error.message}`
        );
    }
}

async function addVideoWithRetry(videoId, maxAttempts) {
    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
        try {
            console.log(`Processing ${videoId} (attempt ${attempt}/${maxAttempts})...`);
            const video = await addVideo(videoId);
            console.log(`Saved: ${video.title} [level: ${video.level}, confidence: ${video.classification?.confidence}]`);
            return;
        } catch (error) {
            const shouldRetry = attempt < maxAttempts && isTransientError(error);
            console.error(`Failed ${videoId}: ${formatError(error)}`);

            if (!shouldRetry) {
                return;
            }

            await sleep(2500 * attempt);
        }
    }
}

async function addVideo(videoId) {
    const response = await fetch(`${API_URL}/admin/add-video`, {
        method: "POST",
        signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
        headers: {
            "Authorization": `Bearer ${ADMIN_TOKEN}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            videoId,
            language: "en",
            status: "published",
            tags: ["seed"]
        })
    });

    const responseText = await response.text();
    const data = parseJsonResponse(responseText);

    if (!response.ok) {
        const error = new Error(data?.error || responseText || `HTTP ${response.status}`);
        error.status = response.status;
        error.details = data;
        throw error;
    }

    return data.video;
}

function parseJsonResponse(text) {
    if (!text) {
        return null;
    }

    try {
        return JSON.parse(text);
    } catch {
        return null;
    }
}

function isTransientError(error) {
    const message = String(error.message || "").toLowerCase();

    return (
        error.status === 429 ||
        error.status === 500 ||
        error.status === 502 ||
        error.status === 503 ||
        message.includes("econnreset") ||
        message.includes("timeout") ||
        message.includes("fetch failed")
    );
}

function formatError(error) {
    const parts = [];

    if (error.status) {
        parts.push(`HTTP ${error.status}`);
    }

    parts.push(error.message || String(error));

    if (error.cause?.code) {
        parts.push(`cause: ${error.cause.code}`);
    }

    return parts.join(" - ");
}

function sleep(ms) {
    return new Promise((resolve) => {
        setTimeout(resolve, ms);
    });
}

seedDatabase().catch((error) => {
    console.error(formatError(error));
    process.exit(1);
});
