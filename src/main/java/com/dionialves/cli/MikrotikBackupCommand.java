package com.dionialves.cli;

import com.dionialves.core.connectors.MikrotikSshConnector;
import com.dionialves.core.service.DeviceLoader;
import com.dionialves.model.Device;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

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

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output"
    )
    private boolean verbose;

    @Override
    public void run() {

        if (verbose) {
            System.out.println("Starting Mikrotik backup...");
            System.out.println("Group ID      : " + groupId);

        }

        List<Device> listOfDevices = null;

        try {
            listOfDevices = DeviceLoader.loadDevices("Mikrotik", groupId, sshPort);
            MikrotikSshConnector mikrotikConnector = new MikrotikSshConnector(username, password);
            mikrotikConnector.backupDevices(listOfDevices);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("Mikrotik backup completed successfully.");
        }
    }
}
