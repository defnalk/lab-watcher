import { Router } from "express";
import { StateDb } from "../db";

/** REST endpoints for browsing processed files. */
export function apiRouter(db: StateDb): Router {
  const r = Router();

  r.get("/files", (_req, res) => {
    res.json({ files: db.recent(50) });
  });

  r.get("/files/:id", (req, res) => {
    const id = Number(req.params.id);
    // The SQLite primary key is a positive integer; reject fractional,
    // negative, zero and non-numeric inputs up front instead of handing
    // them to better-sqlite3 (which would coerce 1.5 -> 1 silently or
    // throw for NaN).
    if (!Number.isInteger(id) || id <= 0) {
      res.status(400).json({ error: "id must be a positive integer" });
      return;
    }
    const row = db.byId(id);
    if (!row) {
      res.status(404).json({ error: "not found" });
      return;
    }
    res.json(row);
  });

  r.get("/stats", (_req, res) => {
    res.json(db.stats());
  });

  return r;
}
