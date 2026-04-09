package com.labwatcher.config;

/** Top-level CLI configuration loaded from {@code lab-watcher.toml}. */
public record AppConfig(
    Watch watch,
    Schema schema,
    State state,
    Notion notion,
    Slack slack,
    Logging logging
) {
    public record Watch(String directory, int intervalSeconds,
                        String filePattern, boolean recursive) {}
    public record Schema(String path) {}
    public record State(String dbPath) {}
    public record Notion(boolean enabled, String token, String databaseId) {}
    public record Slack(boolean enabled, String webhookUrl) {}
    public record Logging(String level, String file) {}

    /** Defaults used when a config file is absent. */
    public static AppConfig defaults() {
        return new AppConfig(
            new Watch("./data", 5, "*.csv", false),
            new Schema("./schemas/mea-pilot-plant.toml"),
            new State("./lab-watcher.db"),
            new Notion(false, "", ""),
            new Slack(false, ""),
            new Logging("INFO", "")
        );
    }
}
