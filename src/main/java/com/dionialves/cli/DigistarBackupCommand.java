package com.dionialves.cli;

import com.dionialves.core.connectors.DigistarSshConnector;
import com.dionialves.core.service.DeviceLoader;
import com.dionialves.model.Device;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(
        name = "digistar",
        description = "Backup Datacom using Zabbix host groups"
)
public class DigistarBackupCommand implements Runnable {

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

    @Option(
            names = {"-t", "--tftp-server"},
            required = true,
            description = "TFTP server"
    )
    private String tftpUrl;

    @Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output"
    )
    private boolean verbose;

    @Override
    public void run() {

        List<Device> listOfDevices = null;
        try {
            listOfDevices = DeviceLoader.loadDevices("Datacom", groupId, sshPort);
            DigistarSshConnector digistarConnector = new DigistarSshConnector(username, password, tftpUrl);
            digistarConnector.backupDevices(listOfDevices);

        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            System.out.println("Datacom backup completed successfully.");
        }
    }
}

