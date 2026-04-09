package com.labwatcher.cmd;

import picocli.CommandLine.Command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

/** {@code lab-watcher init} — drop a starter schema into the current directory. */
@Command(name = "init", description = "Generate a starter schema file in the current directory.")
public final class InitCommand implements Callable<Integer> {

    private static final String STARTER_SCHEMA = """
        name = "mea-pilot-plant"
        version = "1.0"
        require_timestamp_column = true
        timestamp_column_name = "timestamp"
        allow_extra_columns = true

        [[columns]]
        name = "TT101"
        type = "float"
        required = true
        min_value = -10.0
        max_value = 200.0

        [[columns]]
        name = "FT103"
        type = "float"
        required = true
        min_value = 0.0
        max_value = 2000.0
        """;

    @Override
    public Integer call() throws Exception {
        Path out = Path.of("lab-watcher-schema.toml");
        if (Files.exists(out)) {
            System.err.println("refusing to overwrite existing " + out);
            return 1;
        }
        Files.writeString(out, STARTER_SCHEMA, StandardOpenOption.CREATE_NEW);
        System.out.println("wrote " + out);
        return 0;
    }
}
