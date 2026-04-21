package com.labwatcher.config;

import com.moandjiezana.toml.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads {@link AppConfig} from a TOML file with {@code ${VAR}} interpolation. */
public final class ConfigLoader {

    // Match POSIX-style env var names: letters (either case), digits and
    // underscore, not starting with a digit. The previous uppercase-only
    // pattern silently skipped lowercase vars like ${home} or ${my_token}.
    private static final Pattern VAR =
        Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");

    private final Function<String, String> envLookup;

    public ConfigLoader() { this(System::getenv); }

    /** Test seam: inject a fake env. */
    public ConfigLoader(Function<String, String> envLookup) {
        this.envLookup = envLookup;
    }

    /** Load the given path, or return defaults if it doesn't exist. */
    public AppConfig load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return AppConfig.defaults();
        String raw = Files.readString(path);
        return parse(raw);
    }

    /** Parse TOML from a string. Visible for tests. */
    public AppConfig parse(String toml) {
        String expanded = expand(toml);
        Toml t = new Toml().read(expanded);
        AppConfig d = AppConfig.defaults();

        Toml watch = t.getTable("watch");
        AppConfig.Watch w = (watch == null) ? d.watch() : new AppConfig.Watch(
            watch.getString("directory", d.watch().directory()),
            watch.getLong("interval_seconds", (long) d.watch().intervalSeconds()).intValue(),
            watch.getString("file_pattern", d.watch().filePattern()),
            watch.getBoolean("recursive", d.watch().recursive())
        );

        Toml sch = t.getTable("schema");
        AppConfig.Schema s = (sch == null) ? d.schema()
            : new AppConfig.Schema(sch.getString("path", d.schema().path()));

        Toml st = t.getTable("state");
        AppConfig.State stt = (st == null) ? d.state()
            : new AppConfig.State(st.getString("db_path", d.state().dbPath()));

        Toml not = t.getTable("notion");
        AppConfig.Notion n = (not == null) ? d.notion() : new AppConfig.Notion(
            not.getBoolean("enabled", d.notion().enabled()),
            not.getString("token", ""),
            not.getString("database_id", "")
        );

        Toml sl = t.getTable("slack");
        AppConfig.Slack sla = (sl == null) ? d.slack() : new AppConfig.Slack(
            sl.getBoolean("enabled", d.slack().enabled()),
            sl.getString("webhook_url", "")
        );

        Toml lg = t.getTable("logging");
        AppConfig.Logging l = (lg == null) ? d.logging() : new AppConfig.Logging(
            lg.getString("level", d.logging().level()),
            lg.getString("file", "")
        );

        return new AppConfig(w, s, stt, n, sla, l);
    }

    /** Expand {@code ${VAR}} references using the configured env lookup. */
    String expand(String input) {
        Matcher m = VAR.matcher(input);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String v = envLookup.apply(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(v == null ? "" : v));
        }
        m.appendTail(out);
        return out.toString();
    }
}
