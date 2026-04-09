package com.labwatcher.watcher;

import com.labwatcher.dispatch.Dispatcher;
import com.labwatcher.engine.EngineAdapter;
import com.labwatcher.model.FileSummary;
import com.labwatcher.state.ProcessedFile;
import com.labwatcher.state.StateRepository;
import com.labwatcher.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates one CSV file from detection to dispatch:
 * hash → dedup → engine validate → persist → fan out to dispatchers.
 *
 * <p>Work is dispatched onto a virtual-thread executor (Java 21) with a
 * bounded {@link Semaphore} so a burst of file-system events can't spawn
 * unbounded engine calls or exhaust the SQLite write lock. Dispatcher
 * failures are logged but never abort the loop.
 */
public final class FileProcessor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(FileProcessor.class);

    private final EngineAdapter engine;
    private final StateRepository state;
    private final List<Dispatcher> dispatchers;
    private final Path schemaPath;
    private final ExecutorService executor;
    private final Semaphore permits;

    public FileProcessor(EngineAdapter engine, StateRepository state,
                         List<Dispatcher> dispatchers, Path schemaPath) {
        this(engine, state, dispatchers, schemaPath, 8);
    }

    public FileProcessor(EngineAdapter engine, StateRepository state,
                         List<Dispatcher> dispatchers, Path schemaPath,
                         int maxConcurrent) {
        this.engine = engine;
        this.state = state;
        this.dispatchers = List.copyOf(dispatchers);
        this.schemaPath = schemaPath;
        this.permits = new Semaphore(maxConcurrent);
        this.executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("lw-proc-", 0).factory());
    }

    /**
     * Submit a file for asynchronous processing. Blocks the caller only if
     * the bounded permit pool is exhausted (back-pressure on the watcher).
     */
    public void submit(Path file) {
        try {
            permits.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        executor.execute(() -> {
            try {
                process(file);
            } finally {
                permits.release();
            }
        });
    }

    /** Synchronous processing — used by {@code processExisting} and tests. */
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
                state.insert(ProcessedFile.newEntry(
                    file.toString(), file.getFileName().toString(),
                    sha, size, "ERROR", 0, 0, 1, 0));
                return;
            }
            state.insert(ProcessedFile.newEntry(
                file.toString(), file.getFileName().toString(),
                sha, size, status,
                summary.rowCount(), summary.columnCount(),
                summary.errorCount(), summary.warningCount()));

            for (Dispatcher d : dispatchers) {
                try {
                    if (!d.dispatch(summary)) {
                        LOG.warn("dispatcher {} reported failure", d.name());
                    }
                } catch (RuntimeException ex) {
                    LOG.error("dispatcher {} threw: {}", d.name(), ex.getMessage());
                }
            }
        } catch (IOException e) {
            LOG.error("I/O error processing {}: {}", file, e.getMessage());
        }
    }

    /** Drain in-flight work and shut down the executor. */
    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                LOG.warn("executor did not drain in 30s; forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
