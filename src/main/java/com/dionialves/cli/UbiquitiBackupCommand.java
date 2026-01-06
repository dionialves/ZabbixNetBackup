package com.dionialves.cli;

import com.dionialves.core.connectors.UbiquitiSshConnector;
import com.dionialves.core.integration.ZabbixClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;

@Command(
        name = "ubiquiti",
        description = "Backup Ubiquiti using Zabbix host groups"
)
public class UbiquitiBackupCommand implements Runnable {

    @Option(
            names = "--group-id",
            required = true,
            description = "Zabbix host group ID that contains Ubiquiti devices"
    )
    private String groupId;

    @Option(
            names = {"-P", "--ssh-port"},
            defaultValue = "22",
            description = "SSH port (default: ${DEFAULT-VALUE})"
    )
    private int sshPort;

    @Option(
            names = {"-u", "--username"},
            required = true,
            description = "Ubiquiti username"
    )
    private String username;

    @Option(
            names = {"-p", "--password"},
            required = true,
            interactive = true,
            description = "Ubiquiti password"
    )
    private String password;

    @Override
    public void run() {
        try {
            ZabbixClient zabbix = new ZabbixClient();
            zabbix.login();
            List<Map<String, String>> hosts = zabbix.getHostsFromGroup(groupId);

            UbiquitiSshConnector ubiquitiConnector = new UbiquitiSshConnector(username, password, sshPort);
            ubiquitiConnector.backupDevices(hosts);

        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            System.out.println("Ubiquiti backup completed successfully.");
        }
    }
}

