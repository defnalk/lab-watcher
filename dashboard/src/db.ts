import Database from "better-sqlite3";
import fs from "fs";
import { DashboardStats, ProcessedFile } from "./types";

/**
 * Read-only handle to the SQLite database written by the Java watcher.
 * Returns null until the file exists, so the dashboard can start before the
 * watcher does.
 */
export class StateDb {
  private db: Database.Database | null = null;
  constructor(private readonly path: string) {}

  open(): boolean {
    if (this.db) return true;
    if (!fs.existsSync(this.path)) return false;
    this.db = new Database(this.path, { readonly: true, fileMustExist: true });
    return true;
  }

  close(): void {
    this.db?.close();
    this.db = null;
  }

  isOpen(): boolean {
    return this.db !== null;
  }

  recent(limit: number): ProcessedFile[] {
    if (!this.open()) return [];
    return this.db!
      .prepare(
        `SELECT id, file_path, file_name, sha256, file_size, processed_at,
                status, row_count, column_count, error_count, warning_count
         FROM processed_files
         ORDER BY id DESC LIMIT ?`
      )
      .all(limit) as ProcessedFile[];
  }

  byId(id: number): ProcessedFile | undefined {
    if (!this.open()) return undefined;
    return this.db!
      .prepare(`SELECT * FROM processed_files WHERE id = ?`)
      .get(id) as ProcessedFile | undefined;
  }

  /** Rows with id > sinceId, oldest first. Used by the WS poller. */
  newerThan(sinceId: number): ProcessedFile[] {
    if (!this.open()) return [];
    return this.db!
      .prepare(`SELECT * FROM processed_files WHERE id > ? ORDER BY id ASC`)
      .all(sinceId) as ProcessedFile[];
  }

  stats(): DashboardStats {
    if (!this.open()) {
      return {
        totalFiles: 0,
        passCount: 0,
        failCount: 0,
        errorCount: 0,
        passRate: 0,
        lastProcessedAt: null,
      };
    }
    const row = this.db!
      .prepare(
        `SELECT
           COUNT(*) AS total,
           SUM(CASE WHEN status='PASS'  THEN 1 ELSE 0 END) AS pass,
           SUM(CASE WHEN status='FAIL'  THEN 1 ELSE 0 END) AS fail,
           SUM(CASE WHEN status='ERROR' THEN 1 ELSE 0 END) AS err,
           MAX(processed_at) AS last
         FROM processed_files`
      )
      .get() as {
      total: number;
      pass: number | null;
      fail: number | null;
      err: number | null;
      last: string | null;
    };
    const total = row.total ?? 0;
    const pass = row.pass ?? 0;
    return {
      totalFiles: total,
      passCount: pass,
      failCount: row.fail ?? 0,
      errorCount: row.err ?? 0,
      passRate: total > 0 ? pass / total : 0,
      lastProcessedAt: row.last,
    };
  }
}
