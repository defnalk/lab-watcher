package com.labwatcher.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labwatcher.model.ColumnStat;
import com.labwatcher.model.FileSummary;
import com.labwatcher.model.ValidationError;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Java-friendly wrapper around {@link ValEngineJNI}. Calls into the native
 * engine and deserializes the JSON response into immutable record types.
 */
public final class EngineAdapter {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Run parse + validate against the given schema and return a summary. */
    public FileSummary parseAndValidate(Path csv, Path schema) {
        final String json = ValEngineJNI.parseAndValidate(csv.toString(), schema.toString());
        return fromJson(csv.getFileName().toString(), json);
    }

    /** Visible for tests: deserialize a JSON report without invoking JNI. */
    public static FileSummary fromJson(String fileName, String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode parse = root.path("parse_result");

            List<String> headers = new ArrayList<>();
            for (JsonNode h : parse.path("headers")) headers.add(h.asText());

            List<ColumnStat> stats = new ArrayList<>();
            for (JsonNode s : parse.path("column_stats")) {
                stats.add(new ColumnStat(
                    s.path("name").asText(),
                    s.path("type").asText(),
                    s.path("non_null_count").asLong(),
                    s.path("null_count").asLong(),
                    optDouble(s, "min"),
                    optDouble(s, "max"),
                    optDouble(s, "mean"),
                    optDouble(s, "stddev")
                ));
            }

            List<ValidationError> errors = new ArrayList<>();
            for (JsonNode e : root.path("errors")) {
                errors.add(new ValidationError(
                    e.path("line").asInt(),
                    e.path("column").asText(),
                    e.path("code").asText(),
                    e.path("severity").asText(),
                    e.path("message").asText()
                ));
            }

            return new FileSummary(
                fileName,
                root.path("passed").asBoolean(),
                parse.path("total_rows").asLong(),
                headers.size(),
                root.path("error_count").asLong(),
                root.path("warning_count").asLong(),
                headers,
                stats,
                errors
            );
        } catch (Exception ex) {
            throw new IllegalStateException("failed to parse engine JSON: " + ex.getMessage(), ex);
        }
    }

    private static Double optDouble(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        return v.asDouble();
    }

    /** Convenience for callers passing strings. */
    public FileSummary parseAndValidate(String csv, String schema) {
        return parseAndValidate(Paths.get(csv), Paths.get(schema));
    }

    /** Parse-only result: headers + per-column statistics, no validation. */
    public record ParseSummary(List<String> headers, List<ColumnStat> columnStats) {}

    /** Run the engine's parser on a CSV without applying any schema. */
    public ParseSummary parse(Path csv) {
        final String json = ValEngineJNI.parseCsv(csv.toString());
        try {
            JsonNode root = MAPPER.readTree(json);
            List<String> headers = new ArrayList<>();
            for (JsonNode h : root.path("headers")) headers.add(h.asText());
            List<ColumnStat> stats = new ArrayList<>();
            for (JsonNode s : root.path("column_stats")) {
                stats.add(new ColumnStat(
                    s.path("name").asText(),
                    s.path("type").asText(),
                    s.path("non_null_count").asLong(),
                    s.path("null_count").asLong(),
                    optDouble(s, "min"),
                    optDouble(s, "max"),
                    optDouble(s, "mean"),
                    optDouble(s, "stddev")
                ));
            }
            return new ParseSummary(headers, stats);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to parse engine JSON: " + ex.getMessage(), ex);
        }
    }
}
