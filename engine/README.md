# valengine — CSV parser & validator (C++17)

Static library + JNI shared library for CSV parsing, schema validation, and
column statistics. Used by the Java `lab-watcher` CLI.

## Build

```sh
cmake -B build -S . -DCMAKE_BUILD_TYPE=Release
cmake --build build
ctest --test-dir build --output-on-failure
```

This produces:

- `build/libvalengine.a` — static C++ library
- `build/libvalengine.{so,dylib}` — JNI shared library (if a JDK is found)
- `build/valengine_tests` — GoogleTest binary (downloaded via FetchContent)

Compiled with `-Wall -Wextra -Wpedantic -Werror`.

## Public API

| Header                       | Purpose                                       |
|------------------------------|-----------------------------------------------|
| `valengine/types.hpp`        | Shared structs and enums                      |
| `valengine/parser.hpp`       | CSV parsing (file or in-memory)               |
| `valengine/schema.hpp`       | Load schema specs from TOML                   |
| `valengine/validator.hpp`    | Validate parsed CSV against a schema          |
| `valengine/stats.hpp`        | Welford accumulator + per-column statistics   |

## Design notes

### Hand-rolled TOML and JSON

This vertical slice uses a tiny in-house TOML reader (only the schema-file
subset: scalars + `[[columns]]` arrays-of-tables) and hand-written JSON
serialization in the JNI bridge — no `toml11`, no `nlohmann/json`. The
rationale is to keep the dependency surface small while we iterate on the
overall architecture; both can be swapped in via `FetchContent` later
without touching call sites.

### JSON over JNI

The JNI bridge serializes C++ results to a JSON string and returns a single
`jstring`, which the Java side deserializes with Jackson. This is simpler
than mapping every struct field to JNI `Set*Field` calls and far more
maintainable as the data shape evolves. Benchmarking on a 10k-row CSV puts
the JSON cost in the single-digit-millisecond range — irrelevant compared to
disk I/O. The boundary is documented to be JSON-shaped and stable.

### Welford's online variance

`WelfordAccumulator` (in `stats.hpp`) is the standard numerically stable
single-pass algorithm. Don't replace it with naive `Σx²` accumulation.

## Tests

GoogleTest-based, fetched at configure time:

- `test_stats.cpp` — Welford correctness, NaN handling
- `test_parser.cpp` — delimiter detection, quoting, BOM, blank lines
- `test_schema.cpp` — TOML subset parser
- `test_validator.cpp` — required columns, range, type, null checks
