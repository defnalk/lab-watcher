package com.labwatcher.model;

/** Per-column descriptive statistics from the C++ engine. */
public record ColumnStat(
    String name,
    String type,
    long nonNullCount,
    long nullCount,
    Double min,
    Double max,
    Double mean,
    Double stddev
) {}
