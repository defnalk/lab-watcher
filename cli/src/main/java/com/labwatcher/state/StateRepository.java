package com.labwatcher.state;

import java.util.List;
import java.util.Optional;

/** Persistence boundary for processed-file history. */
public interface StateRepository extends AutoCloseable {
    /** Insert a new entry. Returns the row id. */
    long insert(ProcessedFile entry);

    /** Look up a previous entry by file content hash. */
    Optional<ProcessedFile> findByHash(String sha256);

    /** Most recent entries, newest first. */
    List<ProcessedFile> recent(int limit);

    /** Total counts for status reporting. */
    long countByStatus(String status);

    /** Total rows in the table. */
    long total();

    @Override void close();
}
