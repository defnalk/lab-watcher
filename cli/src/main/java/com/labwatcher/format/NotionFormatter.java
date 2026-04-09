package com.labwatcher.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.labwatcher.model.ColumnStat;
import com.labwatcher.model.FileSummary;
import com.labwatcher.model.ValidationError;

import java.time.Instant;

/**
 * Builds the JSON body for {@code POST /v1/pages} that creates a database
 * entry summarising one validated CSV.
 */
public final class NotionFormatter {
    private static final ObjectMapper M = new ObjectMapper();

    /** Build the request body. The caller supplies the database id. */
    public ObjectNode buildPageBody(String databaseId, FileSummary s) {
        ObjectNode root = M.createObjectNode();

        ObjectNode parent = root.putObject("parent");
        parent.put("database_id", databaseId);

        ObjectNode props = root.putObject("properties");
        props.set("File Name", title(s.fileName()));
        props.set("Status", select(statusLabel(s), statusColor(s)));
        props.set("Rows", number(s.rowCount()));
        props.set("Columns", number(s.columnCount()));
        props.set("Errors", number(s.errorCount()));
        props.set("Warnings", number(s.warningCount()));
        props.set("Processed At", date(Instant.now().toString()));

        ArrayNode children = root.putArray("children");
        children.add(heading2("Validation Summary"));
        children.add(callout(
            s.passed() ? "All checks passed ✅" : "Validation failed ❌",
            s.passed() ? "green_background" : "red_background"));

        if (!s.columnStats().isEmpty()) {
            children.add(heading3("Column Statistics"));
            for (ColumnStat cs : s.columnStats()) {
                children.add(bullet(String.format(
                    "%s (%s): min=%s max=%s mean=%s nulls=%d",
                    cs.name(), cs.type(),
                    fmt(cs.min()), fmt(cs.max()), fmt(cs.mean()), cs.nullCount())));
            }
        }
        if (!s.errors().isEmpty()) {
            children.add(heading3("Issues"));
            int shown = 0;
            for (ValidationError e : s.errors()) {
                if (shown++ >= 25) break;
                children.add(bullet(String.format("line %d · %s · %s",
                    e.line(), e.column().isEmpty() ? "-" : e.column(), e.message())));
            }
        }
        return root;
    }

    private static String statusLabel(FileSummary s) { return s.passed() ? "Pass" : "Fail"; }
    private static String statusColor(FileSummary s) { return s.passed() ? "green" : "red"; }

    private static String fmt(Double d) {
        return (d == null || d.isNaN()) ? "-" : String.format("%.2f", d);
    }

    private ObjectNode title(String text) {
        ObjectNode n = M.createObjectNode();
        ArrayNode arr = n.putArray("title");
        ObjectNode t = arr.addObject();
        t.put("type", "text");
        t.putObject("text").put("content", text);
        return n;
    }

    private ObjectNode select(String name, String color) {
        ObjectNode n = M.createObjectNode();
        ObjectNode sel = n.putObject("select");
        sel.put("name", name);
        sel.put("color", color);
        return n;
    }

    private ObjectNode number(long v) {
        ObjectNode n = M.createObjectNode();
        n.put("number", v);
        return n;
    }

    private ObjectNode date(String iso) {
        ObjectNode n = M.createObjectNode();
        n.putObject("date").put("start", iso);
        return n;
    }

    private ObjectNode heading2(String text) { return headingBlock("heading_2", text); }
    private ObjectNode heading3(String text) { return headingBlock("heading_3", text); }

    private ObjectNode headingBlock(String type, String text) {
        ObjectNode n = M.createObjectNode();
        n.put("object", "block");
        n.put("type", type);
        ObjectNode body = n.putObject(type);
        ArrayNode rt = body.putArray("rich_text");
        ObjectNode t = rt.addObject();
        t.put("type", "text");
        t.putObject("text").put("content", text);
        return n;
    }

    private ObjectNode callout(String text, String color) {
        ObjectNode n = M.createObjectNode();
        n.put("object", "block");
        n.put("type", "callout");
        ObjectNode body = n.putObject("callout");
        body.put("color", color);
        ArrayNode rt = body.putArray("rich_text");
        ObjectNode t = rt.addObject();
        t.put("type", "text");
        t.putObject("text").put("content", text);
        return n;
    }

    private ObjectNode bullet(String text) {
        ObjectNode n = M.createObjectNode();
        n.put("object", "block");
        n.put("type", "bulleted_list_item");
        ObjectNode body = n.putObject("bulleted_list_item");
        ArrayNode rt = body.putArray("rich_text");
        ObjectNode t = rt.addObject();
        t.put("type", "text");
        t.putObject("text").put("content", text);
        return n;
    }
}
