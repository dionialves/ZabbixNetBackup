package com.dionialves.core.connectors;

public class MikrotikSshConnector extends DeviceSshConnector {


    public MikrotikSshConnector(String username, String password, int sshPort) {
        super(username, password, sshPort, "mikrotik");
        this.commandForBackup = "/export";
    }
}
