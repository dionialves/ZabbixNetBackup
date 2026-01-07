package com.dionialves.core.connectors;

public class MikrotikService extends DeviceService {


    public MikrotikService(String username, String password, int sshPort) {
        super(username, password, sshPort, "mikrotik");
        this.commandForBackup = "/export";
    }
}
