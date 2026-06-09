# StudyForIELTS Curated Node BFF

Node.js BFF for a curated dictation video feed. It searches YouTube, fetches
transcripts, classifies transcript difficulty into CEFR levels, stores curated
videos in MongoDB, and serves paginated feed results to the Android app.
Transcript fetching tries `youtube-transcript-api` first and falls back to
`youtube-transcript` if the first unofficial provider is unavailable.

## Project Layout

```text
bff/youtube_bff_node/
  src/app.js                    # Express app wiring
  src/server.js                 # Render/local entrypoint
  src/config/db.js              # MongoDB connection
  src/models/CuratedVideo.js    # MongoDB document schema and indexes
  src/routes/adminRoutes.js     # POST /admin/add-video
  src/routes/feedRoutes.js      # GET /feed and GET /feed/:videoId
  src/routes/youtubeRoutes.js   # GET /search and GET /transcript compatibility endpoints
  src/services/cefrClassifier.js
  src/services/youtubeService.js
```

## CEFR Classification Approach

The default classifier is deterministic and local. It combines:

- Flesch-Kincaid readability grade.
- Average sentence length.
- Long-word and academic suffix ratios.
- A small seed CEFR vocabulary list.

This is practical for a free Render/MongoDB deployment because it has no paid
API dependency and is explainable. It is still an estimate, not an official CEFR
assessment. For production quality, keep the saved `computedLevel` but allow
human curation with `levelOverride`, or replace `src/services/cefrClassifier.js`
with an LLM/classifier later while preserving the API contract.

## Local Setup

```powershell
cd "C:\Users\Acer\OneDrive\Documents\Android projects\StudyForIELTS\bff\youtube_bff_node"
npm install
Copy-Item .env.example .env
npm run dev
```

For local learning, run MongoDB with Docker:

```powershell
docker compose up -d
```

This starts:

- MongoDB at `127.0.0.1:27017`.
- Mongo Express at `http://127.0.0.1:8081`.

Mongo Express login:

```text
username: admin
password: admin
```

Use these local database values in `.env`:

```text
MONGODB_URI=mongodb://studyforielts:studyforielts_local_password@127.0.0.1:27017/StudyForIELTS?authSource=admin
MONGODB_DB_NAME=StudyForIELTS
ADMIN_TOKEN=replace-with-a-long-random-secret
```

Then start the Node server:

```powershell
npm run dev
```

Stop the local database:

```powershell
docker compose down
```

Delete the local database volume and all saved videos:

```powershell
docker compose down -v
```

## MongoDB Atlas Free-Tier Setup

Use Atlas for Render/production, not the local Docker URI.

1. Create an Atlas cluster.
2. Create a database user with read/write access.
3. Add your current IP for local testing and Render outbound access as needed.
4. Copy the connection string into `MONGODB_URI`.
5. Use `studyforielts` as the database name, or set `MONGODB_DB_NAME`.

## Endpoints

### Add/Curate A Video

```powershell
Invoke-RestMethod `
  -Method POST `
  -Uri "http://127.0.0.1:8000/admin/add-video" `
  -Headers @{ "x-admin-token" = "replace-with-a-long-random-secret" } `
  -ContentType "application/json" `
  -Body '{"videoId":"dQw4w9WgXcQ","language":"en","tags":["listening"],"status":"published"}'
```

Optional manual override:

```json
{
  "videoId": "dQw4w9WgXcQ",
  "levelOverride": "B2"
}
```

The server fetches the transcript, computes `computedLevel`, stores the final
`level`, and records whether the level came from `computed` or `manual`.

### Public Feed

```powershell
Invoke-RestMethod "http://127.0.0.1:8000/feed?level=B2&page=1&limit=20"
```

The feed response excludes full transcript text for performance. Fetch one
video with transcript segments using:

```powershell
Invoke-RestMethod "http://127.0.0.1:8000/feed/dQw4w9WgXcQ"
```

### Existing Compatibility Endpoints

```powershell
Invoke-RestMethod "http://127.0.0.1:8000/search?q=ielts%20listening%20practice&limit=5"
Invoke-RestMethod "http://127.0.0.1:8000/transcript?videoId=dQw4w9WgXcQ&language=en"
```

## Render Deployment

Create a new Render Web Service with:

```text
Root Directory: bff/youtube_bff_node
Build Command: npm install
Start Command: npm start
Health Check Path: /health
```

Set these Render environment variables:

```text
NODE_ENV=production
MONGODB_URI=your-atlas-uri
MONGODB_DB_NAME=studyforielts
ADMIN_TOKEN=your-long-random-secret
CORS_ALLOW_ORIGINS=*
```

If you want Render Blueprint deployment, add a second service to the repo-root
`render.yaml` instead of replacing the existing Python service:

```yaml
- type: web
  runtime: node
  name: studyforielts-curated-bff
  rootDir: bff/youtube_bff_node
  plan: free
  buildCommand: npm install
  startCommand: npm start
  healthCheckPath: /health
  envVars:
    - key: NODE_ENV
      value: production
    - key: MONGODB_URI
      sync: false
    - key: MONGODB_DB_NAME
      value: studyforielts
    - key: ADMIN_TOKEN
      sync: false
```
