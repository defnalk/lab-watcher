package com.labwatcher.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labwatcher.model.ColumnStat;
import com.labwatcher.model.FileSummary;
import com.labwatcher.model.ValidationError;

import java.time.Instant;

/** Builds a Slack Block Kit JSON payload from a {@link FileSummary}. */
public final class SlackFormatter {
    private static final ObjectMapper M = new ObjectMapper();

    public ObjectNode buildPayload(FileSummary s) {
        ObjectNode root = M.createObjectNode();
        ArrayNode blocks = root.putArray("blocks");

        String emoji = s.passed() ? "✅" : "❌";
        blocks.add(headerBlock(emoji + " " + s.fileName()));
        blocks.add(fieldsSection(s));

        if (!s.columnStats().isEmpty()) {
            blocks.add(mrkdwnSection("*Column Stats:*\n" + columnStatsLine(s)));
        }
        if (!s.errors().isEmpty()) {
            blocks.add(mrkdwnSection("*Errors:*\n" + errorBullets(s)));
        }
        blocks.add(M.createObjectNode().put("type", "divider"));
        blocks.add(contextBlock("Processed at " + Instant.now()));
        return root;
    }

    private static String columnStatsLine(FileSummary s) {
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (ColumnStat cs : s.columnStats()) {
            if (cs.mean() == null || cs.mean().isNaN()) continue;
            if (n++ > 0) b.append('\n');
            b.append("`").append(cs.name()).append("`: ")
             .append(String.format("%.2f", cs.min())).append("–")
             .append(String.format("%.2f", cs.max()))
             .append(" (μ=").append(String.format("%.2f", cs.mean())).append(")");
            if (n >= 6) break;
        }
        return b.length() == 0 ? "_no numeric columns_" : b.toString();
    }

    private static String errorBullets(FileSummary s) {
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (ValidationError e : s.errors()) {
            if (n++ > 0) b.append('\n');
            b.append("• line ").append(e.line()).append(" — ")
             .append(e.column().isEmpty() ? "-" : e.column())
             .append(": ").append(e.message());
            if (n >= 5) {
                if (s.errors().size() > 5) {
                    b.append("\n_…and ").append(s.errors().size() - 5).append(" more_");
                }
                break;
            }
        }
        return b.toString();
    }

    private ObjectNode headerBlock(String text) {
        ObjectNode b = M.createObjectNode();
        b.put("type", "header");
        ObjectNode t = b.putObject("text");
        t.put("type", "plain_text");
        t.put("text", text);
        return b;
    }

    private ObjectNode fieldsSection(FileSummary s) {
        ObjectNode b = M.createObjectNode();
        b.put("type", "section");
        ArrayNode fields = b.putArray("fields");
        fields.add(mrkdwn("*Status:*\n" + (s.passed() ? "✅ Pass" : "❌ Fail")));
        fields.add(mrkdwn("*Rows:*\n" + s.rowCount()));
        fields.add(mrkdwn("*Columns:*\n" + s.columnCount()));
        fields.add(mrkdwn("*Errors:*\n" + s.errorCount()));
        return b;
    }

    private ObjectNode mrkdwnSection(String text) {
        ObjectNode b = M.createObjectNode();
        b.put("type", "section");
        b.set("text", mrkdwn(text));
        return b;
    }

    private ObjectNode mrkdwn(String text) {
        ObjectNode n = M.createObjectNode();
        n.put("type", "mrkdwn");
        n.put("text", text);
        return n;
    }

    private ObjectNode contextBlock(String text) {
        ObjectNode b = M.createObjectNode();
        b.put("type", "context");
        ArrayNode els = b.putArray("elements");
        els.add(mrkdwn(text));
        return b;
    }
}
