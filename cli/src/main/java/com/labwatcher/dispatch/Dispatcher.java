package com.labwatcher.dispatch;

import com.labwatcher.model.FileSummary;

/**
 * Sends a {@link FileSummary} somewhere — terminal, Notion, Slack, etc.
 * Implementations should swallow their own transport errors and log; a
 * dispatch failure must never abort the watcher loop.
 */
public interface Dispatcher {
    /** Short identifier used in logs ({@code "console"}, {@code "notion"}). */
    String name();

    /** Send the summary. Returns false on failure. */
    boolean dispatch(FileSummary summary);
}
