package com.dionialves.cli;

import com.dionialves.core.connectors.DatacomSshConnector;
import com.dionialves.core.exception.ZnbConfigException;
import com.dionialves.core.integration.ZabbixClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;

@Command(
        name = "datacom",
        description = "Backup Datacom using Zabbix host groups"
)
public class DatacomBackupCommand implements Runnable {

    @Option(
            names = "--group-id",
            required = true,
            description = "Zabbix host group ID that contains Datacom devices"
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
            description = "Datacom username"
    )
    private String username;

    @Option(
            names = {"-p", "--password"},
            required = true,
            interactive = true,
            description = "Datacom password"
    )
    private String password;

    @Override
    public void run() {
        try {
            ZabbixClient zabbix = new ZabbixClient();
            zabbix.login();
            List<Map<String, String>> hosts = zabbix.getHostsFromGroup(groupId);

            DatacomSshConnector datacomConnector = new DatacomSshConnector(username, password, sshPort);
            datacomConnector.backupDevices(hosts);

            System.out.println("Backup routine completed");
        } catch (ZnbConfigException e) {
            System.err.println("Erro: " + e.getMessage());
            System.err.println("\nTip: Run 'znb init' to configure Zabbix credentials.");
        } catch (Exception e) {
            System.err.println("\nUnexpected error: " + e.getMessage());
        }
    }
}

