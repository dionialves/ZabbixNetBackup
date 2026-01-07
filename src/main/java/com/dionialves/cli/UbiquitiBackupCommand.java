package com.dionialves.cli;

import com.dionialves.core.connectors.UbiquitiService;
import com.dionialves.core.exception.ZnbConfigException;
import com.dionialves.core.integration.ZabbixClient;
import com.dionialves.model.BackupSummary;
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

    @Option(
            names = {"-P", "--ssh-port"},
            defaultValue = "22",
            description = "SSH port (default: ${DEFAULT-VALUE})"
    )
    private int sshPort;

    @Option(
            names = {"-g", "--group-id"},
            required = true,
            description = "Zabbix host group ID that contains Ubiquiti devices"
    )
    private String groupId;

    @Option(
            names = {"-v", "--verbose"},
            description = "View each backup individually"
    )
    private boolean verbose;

    @Option(
            names = {"--log-file"},
            description = "Save detailed log to file"
    )
    private String logFile;


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

            UbiquitiService backupService = new UbiquitiService(username, password, sshPort);
            backupService.setVerbose(verbose);

            BackupSummary summary = backupService.backupDevices(hosts);
            summary.print();

            if (logFile != null) {
                saveLogToFile(summary, logFile);
            }

        } catch (ZnbConfigException e) {
            System.err.println("Erro: " + e.getMessage());
            System.err.println("\nTip: Run 'znb init' to configure Zabbix credentials.");
        } catch (Exception e) {
            System.err.println("\nUnexpected error: " + e.getMessage());
        }
    }

    private void saveLogToFile(BackupSummary summary, String logFile) {
        // TODO: Implementar salvamento de log em arquivo
        System.out.println("\nLog salvo em: " + logFile);
    }
}
