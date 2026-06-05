import mongoose from "mongoose";

export async function connectMongo() {
  const uri = process.env.MONGODB_URI?.trim();

  if (!uri) {
    throw new Error("MONGODB_URI is required.");
  }

  mongoose.set("strictQuery", true);

  await mongoose.connect(uri, {
    dbName: process.env.MONGODB_DB_NAME?.trim() || undefined
  });

  return mongoose.connection;
}

export async function disconnectMongo() {
  await mongoose.disconnect();
}
