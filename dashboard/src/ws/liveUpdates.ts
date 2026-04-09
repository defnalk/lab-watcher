import { WebSocketServer, WebSocket } from "ws";
import { StateDb } from "../db";
import { WsMessage, ProcessedFile } from "../types";

/**
 * Polls the SQLite db every {@link intervalMs} and pushes any new rows to
 * connected WebSocket clients. Idempotent — tracks the highest seen id.
 */
export class LiveUpdater {
  private wss: WebSocketServer;
  private timer: NodeJS.Timeout | null = null;
  private lastId = 0;

  constructor(
    wss: WebSocketServer,
    private readonly db: StateDb,
    private readonly intervalMs: number = 2000
  ) {
    this.wss = wss;
    this.wss.on("connection", (ws) => this.onConnect(ws));
  }

  start(): void {
    // Seed lastId so we don't replay everything on first poll.
    const recent = this.db.recent(1);
    if (recent.length > 0) this.lastId = recent[0].id;
    this.timer = setInterval(() => this.poll(), this.intervalMs);
  }

  stop(): void {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  private onConnect(ws: WebSocket): void {
    const snapshot: WsMessage = {
      type: "snapshot",
      data: this.db.recent(50),
    };
    ws.send(JSON.stringify(snapshot));
  }

  private poll(): void {
    let rows: ProcessedFile[];
    try {
      rows = this.db.newerThan(this.lastId);
    } catch {
      return;
    }
    for (const row of rows) {
      this.lastId = Math.max(this.lastId, row.id);
      const msg: WsMessage = { type: "new_file", data: row };
      const json = JSON.stringify(msg);
      for (const client of this.wss.clients) {
        if (client.readyState === WebSocket.OPEN) client.send(json);
      }
    }
  }
}
