package com.labwatcher.state;

import java.time.Instant;

/** Row in the processed_files table. */
public record ProcessedFile(
    long id,
    String filePath,
    String fileName,
    String sha256,
    long fileSize,
    Instant processedAt,
    String status,         // PASS, FAIL, ERROR
    long rowCount,
    long columnCount,
    long errorCount,
    long warningCount
) {
    public static ProcessedFile newEntry(
        String filePath, String fileName, String sha256, long fileSize,
        String status, long rowCount, long columnCount,
        long errorCount, long warningCount
    ) {
        return new ProcessedFile(
            0L, filePath, fileName, sha256, fileSize, Instant.now(),
            status, rowCount, columnCount, errorCount, warningCount);
    }
}
