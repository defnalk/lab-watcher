package com.labwatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.labwatcher.format.NotionFormatter;
import com.labwatcher.model.ColumnStat;
import com.labwatcher.model.FileSummary;
import com.labwatcher.model.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotionFormatterTest {

    private static FileSummary failingSummary() {
        return new FileSummary(
            "run.csv", false, 100, 3, 2, 1,
            List.of("timestamp", "TT101", "FT103"),
            List.of(new ColumnStat("TT101", "float", 100, 0, 80.0, 250.0, 165.0, 50.0)),
            List.of(new ValidationError(50, "TT101", "VALUE_OUT_OF_RANGE",
                "ERROR", "value 250 above max 200"))
        );
    }

    @Test void buildsNotionPageBody() {
        JsonNode body = new NotionFormatter().buildPageBody("db-123", failingSummary());

        assertThat(body.path("parent").path("database_id").asText()).isEqualTo("db-123");

        JsonNode props = body.path("properties");
        assertThat(props.path("File Name").path("title").get(0)
            .path("text").path("content").asText()).isEqualTo("run.csv");
        assertThat(props.path("Status").path("select").path("name").asText()).isEqualTo("Fail");
        assertThat(props.path("Status").path("select").path("color").asText()).isEqualTo("red");
        assertThat(props.path("Rows").path("number").asLong()).isEqualTo(100);
        assertThat(props.path("Errors").path("number").asLong()).isEqualTo(2);

        JsonNode children = body.path("children");
        assertThat(children.isArray()).isTrue();
        assertThat(children.size()).isGreaterThanOrEqualTo(3);
        boolean hasCallout = false;
        for (JsonNode b : children) if ("callout".equals(b.path("type").asText())) hasCallout = true;
        assertThat(hasCallout).isTrue();
    }
}
