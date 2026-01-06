package com.dionialves;

import com.dionialves.cli.BackupCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "znb",
        mixinStandardHelpOptions = true,
        version = "znb 0.0.1",
        description = "Network device backup tool powered by Zabbix",
        subcommands = {
                BackupCommand.class
        }
)

public class Main implements Runnable {

    @Override
    public void run() {
        System.out.println("Use 'znb --help' to list available commands.");
    }


    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
