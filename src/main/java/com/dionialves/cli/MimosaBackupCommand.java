package com.dionialves.cli;

import com.dionialves.core.connectors.MimosaHttpConnector;
import com.dionialves.core.integration.ZabbixClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;

@Command(
        name = "mimosa",
        description = "Backup Mimosa using Zabbix host groups"
)
public class MimosaBackupCommand implements Runnable {

    @Option(
            names = "--group-id",
            required = true,
            description = "Zabbix host group ID that contains Mimosa devices"
    )
    private String groupId;

    @Option(
            names = {"-P", "--http-port"},
            defaultValue = "80",
            description = "HTTP port (default: ${DEFAULT-VALUE})"
    )
    private int httpPort;

    @Option(
            names = {"-p", "--password"},
            required = true,
            interactive = true,
            description = "Mimosa password"
    )
    private String password;

    @Override
    public void run() {
        try {

            ZabbixClient zabbix = new ZabbixClient();
            zabbix.login();
            List<Map<String, String>> hosts = zabbix.getHostsFromGroup(groupId);

            MimosaHttpConnector mimosaConnector = new MimosaHttpConnector(password, httpPort);
            mimosaConnector.backupDevices(hosts);

        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            System.out.println("Mimosa backup completed successfully.");
        }
    }
}

