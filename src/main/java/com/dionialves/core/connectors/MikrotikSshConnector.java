package com.dionialves.core.connectors;

public class MikrotikSshConnector extends DeviceSshConnector {


    public MikrotikSshConnector(String username, String password) {
        super(username, password, "mikrotik");
        this.commandForBackup = "/export";
    }
}
