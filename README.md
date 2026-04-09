# lab-watcher

[![CI](https://github.com/defnalk/lab-watcher/actions/workflows/ci.yml/badge.svg)](https://github.com/defnalk/lab-watcher/actions/workflows/ci.yml)

A multi-language CLI for validating lab instrument CSV files against a schema.

- **C++17 engine** (`engine/`) — CSV parsing, schema validation, and column statistics
- **Java 21 CLI** (`cli/`) — user-facing tool that calls the C++ engine via JNI
- **Node.js dashboard** (planned) — live status web UI

This vertical slice implements the C++ engine + JNI bridge + Java `validate`
command end-to-end. Watcher daemon, Notion/Slack dispatchers, and the Node
dashboard are scaffolded for follow-up work.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                Java CLI (com.labwatcher.App)             │
│                                                          │
│   ValidateCommand ─▶ EngineAdapter ─▶ ValEngineJNI       │
│                                            │             │
│                                            ▼ JSON        │
│                                  ┌──────────────────┐    │
│                                  │  C++ valengine   │    │
│                                  │  parser/schema/  │    │
│                                  │  validator/stats │    │
│                                  └──────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

The JNI boundary deliberately uses **JSON-as-string** rather than mapping
every C++ struct field through `Set*Field` calls. The trade-off: a small
serialization cost in exchange for a much smaller, simpler bridge with no
brittle field-name coupling. Validation is I/O-bound, not CPU-bound, so the
serialization is in the noise. See `engine/README.md` for details.

## Quickstart

```sh
# 1. Build the C++ engine + JNI library
cmake -B engine/build -S engine -DCMAKE_BUILD_TYPE=Release
cmake --build engine/build

# 2. Run the C++ tests
ctest --test-dir engine/build --output-on-failure

# 3. Build the Java CLI
cd cli && ./gradlew shadowJar

# 4. Validate a CSV
java -Djava.library.path=../engine/build \
     -jar build/libs/lab-watcher-cli-0.1.0-all.jar \
     validate ../sample-data/mea_pilot_run_001.csv \
     --schema ../schemas/mea-pilot-plant.toml
```

Expected output for the valid sample: a green `✅ PASS` banner with column
statistics. Try `bad_out_of_range.csv` to see range errors.

## Layout

```
lab-watcher/
├── engine/             C++ valengine library + JNI bridge + GoogleTest
├── cli/                Java 21 Picocli CLI (Gradle)
├── schemas/            Example schema TOML files
├── sample-data/        Valid + intentionally-broken CSV fixtures
└── README.md
```

## Why three languages?

- **C++** — CSV parsing and validation are the hot path; static typing and
  zero-cost abstractions keep it fast and predictable.
- **Java** — robust CLI orchestration, mature ecosystem for HTTP/JSON/SQLite,
  great error handling and graceful shutdown semantics.
- **Node.js** (planned) — the lightest-weight way to ship a live web UI that
  reads the same SQLite state file the Java daemon writes.

## Status

| Component                            | State            |
|--------------------------------------|------------------|
| C++ engine (parser/schema/validator) | ✅ implemented   |
| C++ tests (GoogleTest)               | ✅ implemented   |
| JNI bridge                           | ✅ implemented   |
| Java `validate` command              | ✅ implemented   |
| Java tests                           | ✅ implemented   |
| Watcher daemon + SQLite state        | ✅ implemented   |
| `watch` / `status` commands          | ✅ implemented   |
| Config loader (TOML + env vars)      | ✅ implemented   |
| Notion / Slack dispatchers           | ✅ implemented   |
| Node.js dashboard                    | ✅ implemented   |
| GitHub Actions CI (3 parallel jobs)  | ✅ implemented   |
| Docker Compose full stack            | ✅ implemented   |
| Virtual-thread concurrent processing | ✅ implemented   |
| C++ benchmark binary                 | ✅ implemented   |
| Parser fuzz test                     | ✅ implemented   |
| `validate --format json`             | ✅ implemented   |

## Advanced features

A handful of things worth pointing out beyond the basic feature list:

- **Java 21 virtual threads.** `FileProcessor` dispatches each file onto a
  virtual-thread executor with a bounded `Semaphore`. A burst of inotify
  events can't spawn unbounded engine calls or contend on the SQLite write
  lock — the watcher loop back-pressures naturally when permits run out.
- **JSON output mode.** `lab-watcher validate file.csv --schema mea.toml
  --format json` emits a machine-readable summary suitable for piping into
  `jq`, CI gates, or other tooling. Exit code still reflects pass/fail.
- **C++ micro-benchmark.** `engine/build/valengine_bench [rows] [iters]`
  generates a synthetic CSV in memory and reports rows/sec and MB/sec
  throughput. No external benchmark framework — `std::chrono` is enough.
  Reference numbers on an M-series Mac, 10k rows × 6 cols × 50 iters,
  release build: **~986k rows/sec, ~56 MB/sec, 10 ms/iter**.
- **Property-style fuzz test.** `tests/test_fuzz.cpp` feeds 2000 random
  byte strings (with biased BOM/quote/delimiter injection) into the parser
  and asserts no exception escapes — a cheap guard against crashes on
  hostile or corrupt input.
- **Three-job CI.** `.github/workflows/ci.yml` builds and tests the C++
  engine, the Java CLI (with downloaded native artifact), and the Node
  dashboard in parallel, with Gradle and npm caching and artifact upload.
- **Full-stack Docker Compose.** `docker compose up` brings the engine
  builder, Java watcher, and dashboard up against shared volumes for the
  native lib and SQLite state — drop CSVs into `./sample-data` and they
  flow through the entire pipeline.
- **Schema inference.** `lab-watcher infer-schema clean_run.csv -o
  schemas/new.toml` runs the engine over a known-good sample and emits a
  starter schema with min/max bounds derived from the observed column
  statistics, padded by a configurable margin (`--margin 0.10` by default).
  Hand-edit, then point `watch` at the live data directory.
- **SQLite WAL mode.** The state database is opened in WAL mode with
  `synchronous=NORMAL` so the dashboard can poll concurrently with the
  watcher's writes without blocking — important because the dashboard polls
  every 2s for new rows.
- **Prometheus metrics.** The dashboard exposes `/metrics` in Prometheus
  text exposition format (`labwatcher_files_total`,
  `labwatcher_files_by_status{status="…"}`, `labwatcher_pass_rate`,
  `labwatcher_db_connected`) so the watcher can be scraped by an existing
  ops stack with zero extra config.
- **Interactive dashboard rows.** Clicking any row in the dashboard fetches
  `/api/files/:id` and inlines the full record beneath the row — useful
  when triaging failures without leaving the page.
