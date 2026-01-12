package com.dionialves.zabbixnetbackup.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "ubiquiti",
        description = "Backup Digistar devices using Zabbix host groups"
)
public class DigistarBackupCommand extends BaseBackupCommand {

    @Option(
            names = {"-P", "--ssh-port"},
            defaultValue = "22",
            description = "SSH port (default: ${DEFAULT-VALUE})"
    )
    private int sshPort;

    @Option(
            names = {"-t", "--tftp-server"},
            required = true,
            description = "TFTP server"
    )
    private String tftpUrl;

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
        return "/export";
    }
}