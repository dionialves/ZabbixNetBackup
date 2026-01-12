package com.dionialves.zabbixnetbackup.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "ubiquiti",
        description = "Backup Datacom devices using Zabbix host groups"
)
public class DatacomBackupCommand extends BaseBackupCommand {

    @Option(
            names = {"-P", "--ssh-port"},
            defaultValue = "22",
            description = "SSH port (default: ${DEFAULT-VALUE})"
    )
    private int sshPort;

    @Override
    protected String getVendorName() {
        return "datacom";
    }

    @Override
    protected int getPort() {
        return sshPort;
    }

    @Override
    protected String getBackupCommand() {
        return "show running-config | nomore";
    }
}