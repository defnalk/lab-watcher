package com.labwatcher.cmd;

import com.labwatcher.config.AppConfig;
import com.labwatcher.config.ConfigLoader;
import com.labwatcher.engine.EngineAdapter;
import com.labwatcher.format.ConsoleFormatter;
import com.labwatcher.state.SqliteStateRepository;
import com.labwatcher.watcher.DirectoryWatcher;
import com.labwatcher.watcher.FileProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/** {@code lab-watcher watch <directory>} — daemon mode. */
@Command(name = "watch", description = "Watch a directory and validate new CSV files.")
public final class WatchCommand implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(WatchCommand.class);

    @Parameters(index = "0", arity = "0..1",
        description = "Directory to watch (overrides config).")
    private Path directory;

    @Option(names = {"-c", "--config"}, description = "Path to lab-watcher.toml.")
    private Path configPath = Path.of("lab-watcher.toml");

    @Option(names = {"-s", "--schema"}, description = "Schema TOML (overrides config).")
    private Path schemaOverride;

    @Option(names = "--no-color", description = "Disable ANSI colours.")
    private boolean noColor;

    @Override
    public Integer call() throws Exception {
        AppConfig cfg = new ConfigLoader().load(configPath);
        Path watchDir = directory != null ? directory : Path.of(cfg.watch().directory());
        Path schema = schemaOverride != null ? schemaOverride : Path.of(cfg.schema().path());

        if (!Files.isDirectory(watchDir)) {
            System.err.println("error: not a directory: " + watchDir);
            return 2;
        }
        if (!Files.isRegularFile(schema)) {
            System.err.println("error: schema not found: " + schema);
            return 2;
        }

        try (SqliteStateRepository state = SqliteStateRepository.forFile(cfg.state().dbPath())) {
            FileProcessor processor = new FileProcessor(
                new EngineAdapter(), state,
                new ConsoleFormatter(!noColor), schema);

            try (DirectoryWatcher watcher = new DirectoryWatcher(
                    watchDir, cfg.watch().filePattern(), processor)) {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    LOG.info("shutting down...");
                    watcher.close();
                }, "lab-watcher-shutdown"));

                LOG.info("watching {} (pattern={})", watchDir, cfg.watch().filePattern());
                watcher.processExisting();
                watcher.runLoop();
            }
        }
        return 0;
    }
}
