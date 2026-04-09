package com.labwatcher.cmd;

import com.labwatcher.engine.EngineAdapter;
import com.labwatcher.format.ConsoleFormatter;
import com.labwatcher.model.FileSummary;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/** {@code lab-watcher validate <file.csv> --schema <schema.toml>}. */
@Command(name = "validate", description = "Validate a single CSV file against a schema.")
public final class ValidateCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the CSV file to validate.")
    private Path file;

    @Option(names = {"-s", "--schema"}, required = true,
        description = "Path to the schema TOML file.")
    private Path schema;

    @Option(names = "--no-color", description = "Disable ANSI colours in output.")
    private boolean noColor;

    @Override
    public Integer call() {
        if (!Files.isRegularFile(file)) {
            System.err.println("error: file not found: " + file);
            return 2;
        }
        if (!Files.isRegularFile(schema)) {
            System.err.println("error: schema not found: " + schema);
            return 2;
        }
        try {
            EngineAdapter adapter = new EngineAdapter();
            FileSummary summary = adapter.parseAndValidate(file, schema);
            ConsoleFormatter fmt = new ConsoleFormatter(!noColor);
            System.out.print(fmt.format(summary));
            return summary.passed() ? 0 : 1;
        } catch (UnsatisfiedLinkError e) {
            System.err.println("error: native engine not loaded. Build it with:");
            System.err.println("  cmake -B engine/build -S engine && cmake --build engine/build");
            System.err.println("then re-run with -Djava.library.path=engine/build");
            return 3;
        } catch (RuntimeException e) {
            System.err.println("error: " + e.getMessage());
            return 4;
        }
    }
}
