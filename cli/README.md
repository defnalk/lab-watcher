# lab-watcher CLI (Java 21)

Picocli-based command-line tool that validates lab CSV files via the C++
`valengine` library over JNI.

## Build

```sh
./gradlew shadowJar
```

Produces `build/libs/lab-watcher-cli-0.1.0-all.jar` (a fat JAR).

## Run

The C++ JNI library must be on `java.library.path`:

```sh
# from the cli/ directory, after building the engine in ../engine/build
java -Djava.library.path=../engine/build \
     -jar build/libs/lab-watcher-cli-0.1.0-all.jar \
     validate ../sample-data/mea_pilot_run_001.csv \
     --schema ../schemas/mea-pilot-plant.toml
```

Exit codes: `0` pass, `1` fail, `2` bad args, `3` native lib missing,
`4` engine error.

## Commands

- `validate <file.csv> --schema <schema.toml>` — one-shot validation with
  pretty console output.
- `init` — drop a starter schema TOML in the current directory.

(`watch` and `status` commands will land alongside the SQLite state and
dispatcher work.)

## Tests

```sh
./gradlew test
```

The unit tests don't require the native library — `EngineAdapter.fromJson`
is exercised directly with canned JSON.
