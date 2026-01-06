package com.dionialves.cli;

import com.dionialves.core.connectors.MikrotikSshConnector;
import com.dionialves.core.integration.ZabbixClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;

@Command(
        name = "mikrotik",
        description = "Backup Mikrotik devices using Zabbix host groups"
)
public class MikrotikBackupCommand implements Runnable {

    @Option(
            names = "--group-id",
            required = true,
            description = "Zabbix host group ID that contains Mikrotik devices"
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
            description = "Mikrotik username"
    )
    private String username;

    @Option(
            names = {"-p", "--password"},
            required = true,
            interactive = true,
            description = "Mikrotik password"
    )
    private String password;

    @Override
    public void run() {
        try {
            ZabbixClient zabbix = new ZabbixClient();
            zabbix.login();
            List<Map<String, String>> hosts = zabbix.getHostsFromGroup(groupId);

            MikrotikSshConnector mikrotikConnector = new MikrotikSshConnector(username, password, sshPort);
            mikrotikConnector.backupDevices(hosts);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("Mikrotik backup completed successfully.");
        }
    }
}
