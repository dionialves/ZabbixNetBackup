package com.dionialves.core.connectors;

public class MikrotikConnector extends DeviceConnector {


    public MikrotikConnector(String username, String password) {
        super(username, password, "mikrotik");
        this.commandForBackup = "/export";
    }
}
