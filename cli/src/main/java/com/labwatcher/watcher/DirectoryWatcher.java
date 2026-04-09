package com.labwatcher.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Polls a directory with {@link WatchService}, debounces ENTRY_CREATE /
 * ENTRY_MODIFY events, filters by glob pattern, and forwards stable files
 * to a {@link FileProcessor}.
 */
public final class DirectoryWatcher implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DirectoryWatcher.class);
    private static final long DEBOUNCE_MS = 500L;

    private final Path directory;
    private final PathMatcher matcher;
    private final FileProcessor processor;
    private final WatchService ws;
    private volatile boolean running;

    public DirectoryWatcher(Path directory, String glob, FileProcessor processor)
            throws IOException {
        this.directory = directory;
        this.matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        this.processor = processor;
        this.ws = FileSystems.getDefault().newWatchService();
        directory.register(ws,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY);
    }

    /** Process any matching files already present in the directory. */
    public void processExisting() throws IOException {
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> matcher.matches(p.getFileName()))
                  .forEach(processor::process);
        }
    }

    /** Block on the watch loop until {@link #close()} is called. */
    public void runLoop() throws InterruptedException {
        running = true;
        Map<Path, Long> pending = new HashMap<>();
        while (running) {
            WatchKey key = ws.poll(200, TimeUnit.MILLISECONDS);
            long now = System.currentTimeMillis();
            if (key != null) {
                for (var event : key.pollEvents()) {
                    Object ctx = event.context();
                    if (!(ctx instanceof Path rel)) continue;
                    Path abs = directory.resolve(rel);
                    if (!matcher.matches(abs.getFileName())) continue;
                    pending.put(abs, now);
                }
                key.reset();
            }
            // Flush files that have been quiet for DEBOUNCE_MS.
            pending.entrySet().removeIf(e -> {
                if (now - e.getValue() < DEBOUNCE_MS) return false;
                LOG.info("submitting {}", e.getKey().getFileName());
                processor.submit(e.getKey());
                return true;
            });
        }
    }

    @Override
    public void close() {
        running = false;
        try { ws.close(); } catch (IOException ignored) {}
    }
}
