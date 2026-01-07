package com.dionialves.cli;

import com.dionialves.core.connectors.DatacomService;
import com.dionialves.core.exception.ZnbConfigException;
import com.dionialves.core.integration.ZabbixClient;
import com.dionialves.model.BackupSummary;
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
            names = {"-u", "--username"},
            description = "Device User",
            required = true
    )
    private String username;

    @Option(
            names = {"-p", "--password"},
            description = "Device Password",
            interactive = true,
            required = true
    )
    private String password;

    @Option(
            names = {"-g", "--group-id"},
            required = true,
            description = "Zabbix host group ID that contains devices"
    )
    private String groupId;

    @Option(
            names = {"-P", "--ssh-port"},
            defaultValue = "22",
            description = "SSH port (default: ${DEFAULT-VALUE})"
    )
    private int sshPort;

    @Option(
            names = {"-v", "--verbose"},
            description = "View each backup individually"
    )
    private boolean verbose;

    @Override
    public void run() {
        try {
            ZabbixClient zabbix = new ZabbixClient();
            zabbix.login();
            List<Map<String, String>> hosts = zabbix.getHostsFromGroup(groupId);

            if (hosts.isEmpty()) {
                System.err.println("Nenhum dispositivo encontrado no grupo " + groupId);
                return;
            }

            DatacomService backupService = new DatacomService(username, password, sshPort);
            backupService.setVerbose(verbose);

            BackupSummary summary = backupService.backupDevices(hosts);
            summary.print();

        } catch (ZnbConfigException e) {
            System.err.println("Erro: " + e.getMessage());
            System.err.println("\nTip: Run 'znb init' to configure Zabbix credentials.");
        } catch (Exception e) {
            System.err.println("\nUnexpected error: " + e.getMessage());
        }
    }
}

