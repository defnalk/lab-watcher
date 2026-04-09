# lab-watcher dashboard (Node.js + TypeScript)

A read-only web UI that tails the SQLite state file written by the Java
`lab-watcher watch` daemon and pushes new entries to the browser over a
WebSocket.

The frontend is intentionally vanilla HTML/CSS/JS — no React, no bundler.
The TypeScript build only covers the server code.

## Tech

- Node 20, TypeScript strict mode
- Express 4 + `ws` for WebSockets
- `better-sqlite3` for synchronous read-only queries
- Jest + Supertest for tests

## Run

```sh
npm install
npm run build
DB_PATH=../cli/lab-watcher.db PORT=3000 npm start
```

Then open <http://localhost:3000>. If the SQLite file doesn't exist yet
(watcher hasn't started), the dashboard renders "Waiting for lab-watcher…"
and starts populating as soon as the file appears.

## Endpoints

| Method | Path             | Notes                                  |
|--------|------------------|----------------------------------------|
| GET    | `/`              | Serves `public/index.html`             |
| GET    | `/api/files`     | 50 most recent processed files         |
| GET    | `/api/files/:id` | One row by id, 404 if missing          |
| GET    | `/api/stats`     | Totals + pass rate + last processed    |
| GET    | `/health`        | `{status, dbConnected}`                |
| WS     | `/ws`            | Snapshot on connect, then `new_file`s  |

## Test

```sh
npm test
```

The test suite seeds a temporary SQLite file with the same schema the Java
side writes, then exercises every endpoint via Supertest.

## Design notes

- **Read-only DB handle.** The Java CLI owns writes. The dashboard opens
  with `readonly: true, fileMustExist: true`, and the `StateDb` wrapper
  tolerates a missing file by returning empty results until it appears.
- **Polling rather than triggers.** Every 2s the WS layer queries
  `WHERE id > lastSeenId` and pushes any new rows. SQLite has no native
  change feed and the watcher's write rate is low, so polling is the right
  trade-off vs. a more complex notification scheme.
- **Vanilla frontend.** This project lives next to other dashboards in the
  portfolio that use React; here the goal is to show range and to keep the
  dependency tree minimal.
