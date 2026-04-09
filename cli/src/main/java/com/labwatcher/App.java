package com.labwatcher;

import com.labwatcher.cmd.InferSchemaCommand;
import com.labwatcher.cmd.InitCommand;
import com.labwatcher.cmd.StatusCommand;
import com.labwatcher.cmd.ValidateCommand;
import com.labwatcher.cmd.WatchCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** Entry point for the lab-watcher CLI. */
@Command(
    name = "lab-watcher",
    mixinStandardHelpOptions = true,
    version = "lab-watcher 0.1.0",
    description = "Validates lab instrument CSV files against a schema.",
    subcommands = {
        ValidateCommand.class,
        WatchCommand.class,
        StatusCommand.class,
        InferSchemaCommand.class,
        InitCommand.class
    }
)
public final class App implements Runnable {
    @Override public void run() {
        new CommandLine(this).usage(System.out);
    }

    public static void main(String[] args) {
        int rc = new CommandLine(new App()).execute(args);
        System.exit(rc);
    }
}
