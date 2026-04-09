package com.labwatcher.format;

import com.labwatcher.model.ColumnStat;
import com.labwatcher.model.FileSummary;
import com.labwatcher.model.ValidationError;

import java.util.Locale;

/** Renders a {@link FileSummary} as ANSI-coloured terminal output. */
public final class ConsoleFormatter {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";
    private static final String DIM   = "\u001B[2m";

    private final boolean color;

    public ConsoleFormatter(boolean color) { this.color = color; }

    public String format(FileSummary s) {
        StringBuilder b = new StringBuilder();
        String badge = s.passed() ? c(GREEN, "✅ PASS") : c(RED, "❌ FAIL");
        b.append(c(BOLD, badge + "  " + s.fileName())).append('\n');
        b.append(String.format("%d rows · %d columns · %d errors · %d warnings%n",
            s.rowCount(), s.columnCount(), s.errorCount(), s.warningCount()));
        b.append(c(DIM, "─".repeat(60))).append('\n');

        b.append(String.format("%-12s %-10s %10s %10s %10s %8s%n",
            "Column", "Type", "Min", "Max", "Mean", "Nulls"));
        for (ColumnStat cs : s.columnStats()) {
            b.append(String.format(Locale.ROOT, "%-12s %-10s %10s %10s %10s %8d%n",
                truncate(cs.name(), 12),
                cs.type(),
                fmt(cs.min()),
                fmt(cs.max()),
                fmt(cs.mean()),
                cs.nullCount()));
        }

        if (!s.errors().isEmpty()) {
            b.append(c(DIM, "─".repeat(60))).append('\n');
            b.append(c(BOLD, "Issues:")).append('\n');
            int shown = 0;
            for (ValidationError e : s.errors()) {
                if (shown++ >= 20) {
                    b.append(c(DIM, String.format("  … %d more%n", s.errors().size() - 20)));
                    break;
                }
                String tag = "ERROR".equals(e.severity()) ? c(RED, "ERROR") : c(GREEN, "WARN ");
                b.append(String.format("  %s line %d  %s: %s%n",
                    tag, e.line(), e.column().isEmpty() ? "-" : e.column(), e.message()));
            }
        }
        return b.toString();
    }

    private String c(String code, String s) { return color ? code + s + RESET : s; }

    private static String fmt(Double v) {
        if (v == null || v.isNaN()) return "-";
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
