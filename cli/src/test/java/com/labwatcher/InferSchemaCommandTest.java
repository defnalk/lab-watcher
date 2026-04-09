package com.labwatcher;

import com.labwatcher.cmd.InferSchemaCommand;
import com.labwatcher.engine.EngineAdapter.ParseSummary;
import com.labwatcher.model.ColumnStat;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InferSchemaCommandTest {

    private static InferSchemaCommand cmd() throws Exception {
        InferSchemaCommand c = new InferSchemaCommand();
        Field file = InferSchemaCommand.class.getDeclaredField("file");
        file.setAccessible(true);
        file.set(c, Path.of("mea_pilot_run.csv"));
        return c;
    }

    private static ParseSummary sample() {
        return new ParseSummary(
            List.of("timestamp", "TT101", "FT103", "AT101"),
            List.of(
                new ColumnStat("timestamp", "string", 100, 0, null, null, null, null),
                new ColumnStat("TT101", "float", 100, 0, 80.0, 90.0, 85.0, 2.5),
                new ColumnStat("FT103", "float", 100, 0, 850.0, 950.0, 901.0, 25.0),
                new ColumnStat("AT101", "float", 100, 0, 1.0, 3.0, 2.0, 0.5)
            )
        );
    }

    @Test void rendersTomlWithPaddedRanges() throws Exception {
        String toml = cmd().render(sample());
        assertThat(toml).contains("name = \"mea_pilot_run\"".replace("_", "-"));
        assertThat(toml).contains("require_timestamp_column = true");
        assertThat(toml).contains("timestamp_column_name = \"timestamp\"");

        // TT101 range 80–90 with default 10% margin → 79.00–91.00
        assertThat(toml).contains("name = \"TT101\"");
        assertThat(toml).contains("min_value = 79.0000");
        assertThat(toml).contains("max_value = 91.0000");

        // String column should be skipped
        assertThat(toml).doesNotContain("name = \"timestamp\"");
    }

    @Test void omitsTimestampRequirementWhenAbsent() throws Exception {
        ParseSummary p = new ParseSummary(
            List.of("TT101"),
            List.of(new ColumnStat("TT101", "float", 10, 0, 1.0, 2.0, 1.5, 0.5)));
        String toml = cmd().render(p);
        assertThat(toml).contains("require_timestamp_column = false");
    }
}
