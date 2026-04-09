package com.labwatcher.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.labwatcher.model.FileSummary;

/** Serializes a {@link FileSummary} to indented JSON for machine consumption. */
public final class JsonFormatter {
    private final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(FileSummary summary) {
        try {
            return mapper.writeValueAsString(summary) + System.lineSeparator();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize FileSummary", e);
        }
    }
}
