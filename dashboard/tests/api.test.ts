import path from "path";
import os from "os";
import fs from "fs";
import Database from "better-sqlite3";
import request from "supertest";
import { StateDb } from "../src/db";
import { buildApp } from "../src/server";

function seed(dbPath: string): void {
  const db = new Database(dbPath);
  db.exec(`
    CREATE TABLE processed_files (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      file_path TEXT NOT NULL,
      file_name TEXT NOT NULL,
      sha256 TEXT NOT NULL UNIQUE,
      file_size INTEGER NOT NULL,
      processed_at TEXT NOT NULL,
      status TEXT NOT NULL,
      row_count INTEGER,
      column_count INTEGER,
      error_count INTEGER DEFAULT 0,
      warning_count INTEGER DEFAULT 0
    );
  `);
  const stmt = db.prepare(`
    INSERT INTO processed_files
    (file_path, file_name, sha256, file_size, processed_at, status,
     row_count, column_count, error_count, warning_count)
    VALUES (?,?,?,?,?,?,?,?,?,?)
  `);
  stmt.run("/d/a.csv", "a.csv", "h1", 100, "2026-04-09T10:00:00Z", "PASS", 500, 6, 0, 0);
  stmt.run("/d/b.csv", "b.csv", "h2", 200, "2026-04-09T10:01:00Z", "FAIL", 100, 6, 3, 1);
  stmt.run("/d/c.csv", "c.csv", "h3", 150, "2026-04-09T10:02:00Z", "PASS", 250, 6, 0, 0);
  db.close();
}

describe("dashboard API", () => {
  let dbPath: string;
  let db: StateDb;

  beforeAll(() => {
    dbPath = path.join(os.tmpdir(), `labw-${Date.now()}.db`);
    seed(dbPath);
    db = new StateDb(dbPath);
  });

  afterAll(() => {
    db.close();
    fs.unlinkSync(dbPath);
  });

  test("GET /health reports db connected", async () => {
    const res = await request(buildApp(db)).get("/health");
    expect(res.status).toBe(200);
    expect(res.body.dbConnected).toBe(true);
  });

  test("GET /api/files returns rows newest first", async () => {
    const res = await request(buildApp(db)).get("/api/files");
    expect(res.status).toBe(200);
    expect(res.body.files).toHaveLength(3);
    expect(res.body.files[0].file_name).toBe("c.csv");
  });

  test("GET /api/stats computes pass rate", async () => {
    const res = await request(buildApp(db)).get("/api/stats");
    expect(res.status).toBe(200);
    expect(res.body.totalFiles).toBe(3);
    expect(res.body.passCount).toBe(2);
    expect(res.body.failCount).toBe(1);
    expect(res.body.passRate).toBeCloseTo(2 / 3, 5);
  });

  test("GET /api/files/:id returns single row", async () => {
    const res = await request(buildApp(db)).get("/api/files/2");
    expect(res.status).toBe(200);
    expect(res.body.file_name).toBe("b.csv");
  });

  test("GET /api/files/:id 404s on unknown id", async () => {
    const res = await request(buildApp(db)).get("/api/files/9999");
    expect(res.status).toBe(404);
  });

  test("GET /metrics returns Prometheus exposition", async () => {
    const res = await request(buildApp(db)).get("/metrics");
    expect(res.status).toBe(200);
    expect(res.headers["content-type"]).toContain("text/plain");
    expect(res.text).toContain("labwatcher_files_total 3");
    expect(res.text).toContain('labwatcher_files_by_status{status="pass"} 2');
    expect(res.text).toContain('labwatcher_files_by_status{status="fail"} 1');
    expect(res.text).toContain("labwatcher_pass_rate");
    expect(res.text).toContain("labwatcher_db_connected 1");
  });
});
