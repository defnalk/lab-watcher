import { Router } from "express";
import { StateDb } from "../db";

/**
 * Prometheus-style plaintext metrics endpoint. Mounts at /metrics and
 * exposes counters scraped directly from SQLite stats. Cheap to call —
 * one aggregation query per request.
 */
export function metricsRouter(db: StateDb): Router {
  const r = Router();
  r.get("/metrics", (_req, res) => {
    const s = db.stats();
    const lines = [
      "# HELP labwatcher_files_total Total CSV files processed.",
      "# TYPE labwatcher_files_total counter",
      `labwatcher_files_total ${s.totalFiles}`,
      "# HELP labwatcher_files_by_status Files processed, partitioned by status.",
      "# TYPE labwatcher_files_by_status counter",
      `labwatcher_files_by_status{status="pass"} ${s.passCount}`,
      `labwatcher_files_by_status{status="fail"} ${s.failCount}`,
      `labwatcher_files_by_status{status="error"} ${s.errorCount}`,
      "# HELP labwatcher_pass_rate Fraction of files that passed validation.",
      "# TYPE labwatcher_pass_rate gauge",
      `labwatcher_pass_rate ${s.passRate.toFixed(6)}`,
      "# HELP labwatcher_db_connected 1 if the SQLite state file is open.",
      "# TYPE labwatcher_db_connected gauge",
      `labwatcher_db_connected ${db.isOpen() ? 1 : 0}`,
      "",
    ];
    res.type("text/plain; version=0.0.4").send(lines.join("\n"));
  });
  return r;
}
