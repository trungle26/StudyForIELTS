import mongoose from "mongoose";

export const CEFR_LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"];

const transcriptSegmentSchema = new mongoose.Schema(
  {
    startTime: { type: Number, required: true },
    endTime: { type: Number, required: true },
    text: { type: String, required: true }
  },
  { _id: false }
);

const classificationSchema = new mongoose.Schema(
  {
    level: { type: String, enum: CEFR_LEVELS, required: true },
    confidence: { type: Number, min: 0, max: 1, required: true },
    metrics: { type: mongoose.Schema.Types.Mixed, required: true },
    explanation: { type: String, required: true },
    classifierVersion: { type: String, required: true }
  },
  { _id: false }
);

const curatedVideoSchema = new mongoose.Schema(
  {
    videoId: { type: String, required: true, unique: true, index: true },
    title: { type: String, required: true },
    description: { type: String, default: "" },
    channelId: { type: String, default: "" },
    channelTitle: { type: String, default: "" },
    publishDate: { type: Date },
    durationSeconds: { type: Number },
    thumbnailUrl: { type: String, default: "" },
    embedUrl: { type: String, default: "" },
    language: { type: String, default: "en" },
    transcriptSegments: { type: [transcriptSegmentSchema], default: [] },
    transcriptText: { type: String, required: true },
    wordCount: { type: Number, required: true },
    level: { type: String, enum: CEFR_LEVELS, required: true, index: true },
    computedLevel: { type: String, enum: CEFR_LEVELS, required: true },
    levelSource: { type: String, enum: ["computed", "manual"], default: "computed" },
    classification: { type: classificationSchema, required: true },
    tags: { type: [String], default: [] },
    status: { type: String, enum: ["published", "draft"], default: "published", index: true },
    curatedAt: { type: Date, default: Date.now }
  },
  { timestamps: true }
);

curatedVideoSchema.index({ level: 1, status: 1, curatedAt: -1 });

export const CuratedVideo = mongoose.model("CuratedVideo", curatedVideoSchema);
