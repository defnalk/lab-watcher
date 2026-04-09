package com.labwatcher;

import com.labwatcher.engine.EngineAdapter;
import com.labwatcher.format.ConsoleFormatter;
import com.labwatcher.model.FileSummary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EngineAdapterTest {

    private static final String SAMPLE_JSON = """
        {"passed":false,"error_count":1,"warning_count":0,
         "errors":[{"line":3,"column":"TT101","code":"VALUE_OUT_OF_RANGE",
                    "severity":"ERROR","message":"value 250 above max 200"}],
         "parse_result":{"success":true,"error_message":"",
            "total_rows":2,"valid_rows":2,"skipped_rows":0,
            "headers":["timestamp","TT101"],
            "column_stats":[
              {"name":"timestamp","type":"string","non_null_count":2,"null_count":0,
               "min":null,"max":null,"mean":null,"stddev":null},
              {"name":"TT101","type":"float","non_null_count":2,"null_count":0,
               "min":85.2,"max":250.0,"mean":167.6,"stddev":116.5}
            ]}}
        """;

    @Test void deserializesJsonReport() {
        FileSummary s = EngineAdapter.fromJson("sample.csv", SAMPLE_JSON);
        assertThat(s.passed()).isFalse();
        assertThat(s.fileName()).isEqualTo("sample.csv");
        assertThat(s.rowCount()).isEqualTo(2);
        assertThat(s.columnCount()).isEqualTo(2);
        assertThat(s.errorCount()).isEqualTo(1);
        assertThat(s.errors()).singleElement()
            .satisfies(e -> {
                assertThat(e.column()).isEqualTo("TT101");
                assertThat(e.code()).isEqualTo("VALUE_OUT_OF_RANGE");
                assertThat(e.line()).isEqualTo(3);
            });
        assertThat(s.columnStats()).hasSize(2);
        assertThat(s.columnStats().get(1).mean()).isEqualTo(167.6);
        assertThat(s.columnStats().get(0).min()).isNull();
    }

    @Test void consoleFormatterRenders() {
        FileSummary s = EngineAdapter.fromJson("sample.csv", SAMPLE_JSON);
        String out = new ConsoleFormatter(false).format(s);
        assertThat(out).contains("FAIL", "sample.csv", "TT101", "VALUE_OUT_OF_RANGE");
    }
}
