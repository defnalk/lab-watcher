package com.labwatcher.dispatch;

import com.labwatcher.format.ConsoleFormatter;
import com.labwatcher.model.FileSummary;

/** Pretty-prints summaries to stdout via {@link ConsoleFormatter}. */
public final class ConsoleDispatcher implements Dispatcher {
    private final ConsoleFormatter formatter;

    public ConsoleDispatcher(ConsoleFormatter formatter) {
        this.formatter = formatter;
    }

    @Override public String name() { return "console"; }

    @Override
    public boolean dispatch(FileSummary summary) {
        System.out.print(formatter.format(summary));
        return true;
    }
}
