import "dotenv/config";
import { createServer } from "node:http";
import { createApp } from "./app.js";
import { connectMongo, disconnectMongo } from "./config/db.js";

const port = Number(process.env.PORT || 8000);

async function start() {
  await connectMongo();

  const app = createApp();
  const server = createServer(app);

  server.listen(port, "0.0.0.0", () => {
    console.log(`Curated BFF listening on port ${port}`);
  });

  for (const signal of ["SIGINT", "SIGTERM"]) {
    process.on(signal, async () => {
      server.close(async () => {
        await disconnectMongo();
        process.exit(0);
      });
    });
  }
}

start().catch((error) => {
  console.error(error);
  process.exit(1);
});
