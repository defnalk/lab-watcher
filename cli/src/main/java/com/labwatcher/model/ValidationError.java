package com.labwatcher.model;

/** A single validation issue surfaced by the engine. */
public record ValidationError(
    int line,
    String column,
    String code,
    String severity,
    String message
) {}
