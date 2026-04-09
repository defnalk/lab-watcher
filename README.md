# lab-watcher

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
| Notion / Slack dispatchers           | 🚧 not yet       |
| Node.js dashboard                    | 🚧 not yet       |
