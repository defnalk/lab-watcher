import express from "express";
import cors from "cors";
import http from "http";
import path from "path";
import { WebSocketServer } from "ws";
import { StateDb } from "./db";
import { apiRouter } from "./routes/api";
import { healthRouter } from "./routes/health";
import { metricsRouter } from "./routes/metrics";
import { LiveUpdater } from "./ws/liveUpdates";

/** Build the Express app. Exported so tests can mount it without spawning a port. */
export function buildApp(db: StateDb): express.Express {
  const app = express();
  app.use(cors());
  app.use(express.json());
  app.use("/api", apiRouter(db));
  app.use(healthRouter(db));
  app.use(metricsRouter(db));
  app.use(express.static(path.join(__dirname, "..", "public")));
  return app;
}

function main(): void {
  const dbPath = process.env.DB_PATH ?? "../cli/lab-watcher.db";
  const port = Number(process.env.PORT ?? 3000);
  const db = new StateDb(dbPath);

  const app = buildApp(db);
  const server = http.createServer(app);
  const wss = new WebSocketServer({ server, path: "/ws" });
  const live = new LiveUpdater(wss, db);
  live.start();

  server.listen(port, () => {
    console.log(`lab-watcher dashboard listening on http://localhost:${port}`);
    console.log(`db: ${dbPath}`);
  });

  const shutdown = (): void => {
    console.log("\nshutting down...");
    live.stop();
    wss.close();
    server.close(() => {
      db.close();
      process.exit(0);
    });
  };
  process.on("SIGINT", shutdown);
  process.on("SIGTERM", shutdown);
}

if (require.main === module) main();
