package com.dionialves.cli;

import com.dionialves.core.connectors.MimosaHttpConnector;
import com.dionialves.core.service.DeviceLoader;
import com.dionialves.model.Device;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

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

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output"
    )
    private boolean verbose;

    @Override
    public void run() {

        List<Device> listOfDevices = null;
        try {
            listOfDevices = DeviceLoader.loadDevices("Datacom", groupId, httpPort);
            MimosaHttpConnector mimosaConnector = new MimosaHttpConnector(password);
            mimosaConnector.backupDevices(listOfDevices);

        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            System.out.println("Mimosa backup completed successfully.");
        }
    }
}

