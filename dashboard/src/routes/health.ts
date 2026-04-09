import { Router } from "express";
import { StateDb } from "../db";

export function healthRouter(db: StateDb): Router {
  const r = Router();
  r.get("/health", (_req, res) => {
    res.json({ status: "ok", dbConnected: db.open() });
  });
  return r;
}
