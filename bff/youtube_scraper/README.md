# YouTube BFF API

FastAPI backend for the IELTS Dictation Android app. It is deployable as a
small standalone service from `bff/youtube_scraper`.

It exposes:

- `GET /search?q=ielts listening practice&limit=10`
- `GET /transcript?videoId=dQw4w9WgXcQ&language=en`
- `GET /health`

Legacy local routes are still available but hidden from the OpenAPI schema:

- `GET /api/youtube/search?q=ielts listening practice&limit=10`
- `GET /api/youtube/captions/{videoId}?language=en`

The script does not use official YouTube Data API tokens. It uses `yt-dlp` for
search and `youtube-transcript-api` for subtitles. Both rely on undocumented
YouTube web behavior, so keep dependencies updated and avoid high-volume
scraping from a single IP.

## Project Layout

```text
bff/youtube_scraper/
  main.py              # FastAPI app used by local, Render, and Vercel
  requirements.txt     # Runtime dependencies
  vercel.json          # Vercel function bundle config
  .python-version      # Python version for Vercel and local tools
render.yaml            # Render Blueprint at the repository root
```

## Windows Setup

Run these commands in PowerShell from the repo root:

```powershell
cd "C:\Users\Acer\OneDrive\Documents\Android projects\StudyForIELTS\bff\youtube_scraper"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
uvicorn main:app --reload --host 127.0.0.1 --port 8000
```

If `python` is not available but the Python Launcher is installed, use:

```powershell
py -3.12 -m venv .venv
```

If PowerShell blocks activation, run:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
.\.venv\Scripts\Activate.ps1
```

## Test Locally

Open these URLs in a browser or call them from PowerShell:

```powershell
Invoke-RestMethod "http://127.0.0.1:8000/health"
Invoke-RestMethod "http://127.0.0.1:8000/search?q=ielts%20listening%20practice&limit=5"
Invoke-RestMethod "http://127.0.0.1:8000/transcript?videoId=dQw4w9WgXcQ&language=en"
```

Expected transcript shape:

```json
{
  "videoId": "dQw4w9WgXcQ",
  "language": "English",
  "languageCode": "en",
  "isGenerated": false,
  "segments": [
    {
      "startTime": 0.0,
      "endTime": 1.54,
      "text": "caption text"
    }
  ]
}
```

For an Android emulator, `127.0.0.1` points to the emulator itself. Use:

```text
http://10.0.2.2:8000
```

For a physical phone on the same Wi-Fi, run the server with:

```powershell
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

Then call `http://YOUR_PC_LAN_IP:8000`.

## Configuration

Optional environment variables:

```text
CORS_ALLOW_ORIGINS=*
DEFAULT_SEARCH_LIMIT=10
MAX_SEARCH_LIMIT=25
SEARCH_SOCKET_TIMEOUT_SECONDS=12
```

For a public production API, replace `CORS_ALLOW_ORIGINS=*` with your web client
origins where applicable. Android native app requests do not use browser CORS,
but keeping this configurable avoids reopening code later.

## Deploy to Render

The repository root contains `render.yaml`, so Render can create the service
from a Blueprint. It sets:

- `rootDir: bff/youtube_scraper`
- `buildCommand: pip install -r requirements.txt`
- `startCommand: uvicorn main:app --host 0.0.0.0 --port $PORT`
- `healthCheckPath: /health`

Steps:

1. Push this repo to GitHub/GitLab/Bitbucket.
2. In Render, create a new Blueprint from the repo.
3. Confirm the `studyforielts-youtube-bff` service and deploy.
4. Call `https://studyforielts-youtube-bff.onrender.com/health`.

If you create a Render Web Service manually instead of using the Blueprint, set
the root directory to `bff/youtube_scraper`, use the same build/start commands
above, and set `PYTHON_VERSION=3.12.11`.


Render is usually a better fit for this scraper than Vercel because the service
can run as a normal long-lived web process. Vercel works for light traffic, but
serverless cold starts and function time limits are less forgiving for scraping.

## Notes

- Only videos with available English captions will return segments.
- Auto-generated captions can be returned when YouTube exposes them.
- Age-restricted, region-blocked, or captions-disabled videos may fail.
- YouTube can throttle or block hosted datacenter IPs. Add caching, rate limiting, request logging, and a proxy strategy before exposing this to significant public traffic.
