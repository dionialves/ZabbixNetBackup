package com.dionialves.zabbixnetbackup.cli;

import com.dionialves.core.exception.ZnbConfigException;
import com.dionialves.core.integration.ZabbixClient;
import com.dionialves.core.service.MimosaService;
import com.dionialves.model.BackupSummary;
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
            names = {"-p", "--password"},
            required = true,
            interactive = true,
            description = "Mimosa password"
    )
    private String password;

    @Option(
            names = {"-P", "--http-port"},
            defaultValue = "80",
            description = "HTTP port (default: ${DEFAULT-VALUE})"
    )
    private int httpPort;

    @Option(
            names = {"-g", "--group-id"},
            required = true,
            description = "Zabbix host group ID that contains Mimosa devices"
    )
    private String groupId;

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

            MimosaService backupService = new MimosaService( password, httpPort);

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

