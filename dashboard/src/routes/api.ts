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
    if (!Number.isFinite(id)) {
      res.status(400).json({ error: "id must be a number" });
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
