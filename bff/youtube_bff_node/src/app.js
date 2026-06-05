import cors from "cors";
import express from "express";
import helmet from "helmet";
import { adminRouter } from "./routes/adminRoutes.js";
import { feedRouter } from "./routes/feedRoutes.js";
import { youtubeRouter } from "./routes/youtubeRoutes.js";

export function createApp() {
  const app = express();

  app.use(helmet());
  app.use(cors({ origin: parseCorsOrigins() }));
  app.use(express.json({ limit: "2mb" }));

  app.get("/health", (_req, res) => {
    res.json({ status: "ok" });
  });

  app.use("/", youtubeRouter);
  app.use("/feed", feedRouter);
  app.use("/admin", adminRouter);

  app.use((req, res) => {
    res.status(404).json({ error: `Route not found: ${req.method} ${req.path}` });
  });

  app.use((error, _req, res, _next) => {
    const status = error.status || error.statusCode || 500;
    const message = status >= 500 ? "Internal server error." : error.message;

    if (status >= 500) {
      console.error(error);
    }

    res.status(status).json({ error: message });
  });

  return app;
}

function parseCorsOrigins() {
  const raw = process.env.CORS_ALLOW_ORIGINS || "*";

  if (raw === "*") {
    return "*";
  }

  return raw
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean);
}
