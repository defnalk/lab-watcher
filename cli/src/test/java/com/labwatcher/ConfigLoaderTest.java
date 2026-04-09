package com.labwatcher;

import com.labwatcher.config.AppConfig;
import com.labwatcher.config.ConfigLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    private static final String SAMPLE = """
        [watch]
        directory = "./data"
        interval_seconds = 10
        file_pattern = "*.csv"
        recursive = false

        [schema]
        path = "./schemas/mea.toml"

        [state]
        db_path = "./lw.db"

        [notion]
        enabled = true
        token = "${LW_TOKEN}"
        database_id = "db-1"

        [slack]
        enabled = false
        webhook_url = ""

        [logging]
        level = "DEBUG"
        """;

    @Test void parsesAllSections() {
        AppConfig cfg = new ConfigLoader(k -> Map.of("LW_TOKEN", "secret-xyz").get(k))
            .parse(SAMPLE);
        assertThat(cfg.watch().directory()).isEqualTo("./data");
        assertThat(cfg.watch().intervalSeconds()).isEqualTo(10);
        assertThat(cfg.schema().path()).isEqualTo("./schemas/mea.toml");
        assertThat(cfg.state().dbPath()).isEqualTo("./lw.db");
        assertThat(cfg.notion().enabled()).isTrue();
        assertThat(cfg.notion().token()).isEqualTo("secret-xyz");
        assertThat(cfg.notion().databaseId()).isEqualTo("db-1");
        assertThat(cfg.logging().level()).isEqualTo("DEBUG");
    }

    @Test void missingEnvVarBecomesEmpty() {
        AppConfig cfg = new ConfigLoader(k -> null).parse(SAMPLE);
        assertThat(cfg.notion().token()).isEmpty();
    }

    @Test void defaultsWhenNoFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp)
            throws Exception {
        AppConfig cfg = new ConfigLoader().load(tmp.resolve("nope.toml"));
        assertThat(cfg.watch().directory()).isEqualTo("./data");
    }
}
