package com.labwatcher.watcher;

import com.labwatcher.engine.EngineAdapter;
import com.labwatcher.format.ConsoleFormatter;
import com.labwatcher.model.FileSummary;
import com.labwatcher.state.ProcessedFile;
import com.labwatcher.state.StateRepository;
import com.labwatcher.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Orchestrates one CSV file from detection to dispatch:
 * hash → dedup → engine validate → persist → console output.
 */
public final class FileProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(FileProcessor.class);

    private final EngineAdapter engine;
    private final StateRepository state;
    private final ConsoleFormatter formatter;
    private final Path schemaPath;

    public FileProcessor(EngineAdapter engine, StateRepository state,
                         ConsoleFormatter formatter, Path schemaPath) {
        this.engine = engine;
        this.state = state;
        this.formatter = formatter;
        this.schemaPath = schemaPath;
    }

    /** Process a single file. Idempotent: dedups by content hash. */
    public void process(Path file) {
        try {
            if (!Files.isRegularFile(file)) return;
            String sha = HashUtil.sha256(file);
            if (state.findByHash(sha).isPresent()) {
                LOG.debug("skip already-processed {} (sha={})", file.getFileName(), sha);
                return;
            }
            long size = Files.size(file);
            FileSummary summary;
            String status;
            try {
                summary = engine.parseAndValidate(file, schemaPath);
                status = summary.passed() ? "PASS" : "FAIL";
            } catch (RuntimeException ex) {
                LOG.error("engine error on {}: {}", file, ex.getMessage());
                ProcessedFile err = ProcessedFile.newEntry(
                    file.toString(), file.getFileName().toString(),
                    sha, size, "ERROR", 0, 0, 1, 0);
                state.insert(err);
                return;
            }
            state.insert(ProcessedFile.newEntry(
                file.toString(), file.getFileName().toString(),
                sha, size, status,
                summary.rowCount(), summary.columnCount(),
                summary.errorCount(), summary.warningCount()));
            System.out.print(formatter.format(summary));
        } catch (IOException e) {
            LOG.error("I/O error processing {}: {}", file, e.getMessage());
        }
    }
}
