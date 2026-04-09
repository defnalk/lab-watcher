package com.labwatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.labwatcher.format.SlackFormatter;
import com.labwatcher.model.ColumnStat;
import com.labwatcher.model.FileSummary;
import com.labwatcher.model.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlackFormatterTest {

    private static FileSummary passing() {
        return new FileSummary(
            "ok.csv", true, 500, 6, 0, 0,
            List.of("timestamp", "TT101"),
            List.of(new ColumnStat("TT101", "float", 500, 0, 80.1, 89.7, 85.2, 2.1)),
            List.of()
        );
    }

    private static FileSummary failing() {
        return new FileSummary(
            "bad.csv", false, 100, 3, 2, 0,
            List.of("timestamp", "TT101"),
            List.of(),
            List.of(
                new ValidationError(10, "TT101", "VALUE_OUT_OF_RANGE", "ERROR", "above max"),
                new ValidationError(20, "FT103", "TYPE_MISMATCH", "ERROR", "not a number"))
        );
    }

    @Test void passingPayloadHasGreenStatus() {
        JsonNode payload = new SlackFormatter().buildPayload(passing());
        JsonNode blocks = payload.path("blocks");
        assertThat(blocks.isArray()).isTrue();
        assertThat(blocks.get(0).path("type").asText()).isEqualTo("header");
        assertThat(blocks.get(0).path("text").path("text").asText()).contains("ok.csv");
        // Status field
        JsonNode fields = blocks.get(1).path("fields");
        assertThat(fields.get(0).path("text").asText()).contains("Pass");
    }

    @Test void failingPayloadIncludesErrors() {
        JsonNode payload = new SlackFormatter().buildPayload(failing());
        boolean hasErrors = false;
        for (JsonNode b : payload.path("blocks")) {
            if ("section".equals(b.path("type").asText())
                && b.path("text").path("text").asText().contains("Errors")) {
                hasErrors = true;
            }
        }
        assertThat(hasErrors).isTrue();
    }
}
