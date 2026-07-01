package com.dionialves.cli;

import picocli.CommandLine.Command;

@Command(
        name = "backup",
        mixinStandardHelpOptions = true,
        description = "Run backup operations for network devices",
        subcommands = {
                MikrotikBackupCommand.class,
                CiscoBackupCommand.class,
                DatacomBackupCommand.class,
                DigistarBackupCommand.class,
                MimosaBackupCommand.class,
                UbiquitiBackupCommand.class
        }
)

public class BackupCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Please specify a vendor.");
        System.out.println("Example: znb backup mikrotik --group-id 209");
    }
}

