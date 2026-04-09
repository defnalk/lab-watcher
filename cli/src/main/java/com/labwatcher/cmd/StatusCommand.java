package com.labwatcher.cmd;

import com.labwatcher.config.AppConfig;
import com.labwatcher.config.ConfigLoader;
import com.labwatcher.state.ProcessedFile;
import com.labwatcher.state.SqliteStateRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/** {@code lab-watcher status} — print recent processing history. */
@Command(name = "status", description = "Show recent processed files.")
public final class StatusCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, description = "Path to lab-watcher.toml.")
    private Path configPath = Path.of("lab-watcher.toml");

    @Option(names = "--last", description = "How many entries to show (default: 10).")
    private int last = 10;

    @Override
    public Integer call() throws Exception {
        AppConfig cfg = new ConfigLoader().load(configPath);
        try (SqliteStateRepository state =
                 SqliteStateRepository.forFile(cfg.state().dbPath())) {
            long total = state.total();
            long pass = state.countByStatus("PASS");
            long fail = state.countByStatus("FAIL");
            long err  = state.countByStatus("ERROR");
            System.out.printf("total=%d  pass=%d  fail=%d  error=%d%n", total, pass, fail, err);
            System.out.println("─".repeat(80));
            System.out.printf("%-30s %-6s %6s %6s %8s   %s%n",
                "file", "status", "rows", "errs", "size", "processed");
            List<ProcessedFile> rows = state.recent(last);
            for (ProcessedFile p : rows) {
                System.out.printf("%-30s %-6s %6d %6d %8d   %s%n",
                    truncate(p.fileName(), 30),
                    p.status(),
                    p.rowCount(),
                    p.errorCount(),
                    p.fileSize(),
                    p.processedAt());
            }
        }
        return 0;
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
