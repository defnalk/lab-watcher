package com.labwatcher.model;

import java.util.List;

/** A complete validation outcome for a single CSV file. */
public record FileSummary(
    String fileName,
    boolean passed,
    long rowCount,
    long columnCount,
    long errorCount,
    long warningCount,
    List<String> headers,
    List<ColumnStat> columnStats,
    List<ValidationError> errors
) {}
