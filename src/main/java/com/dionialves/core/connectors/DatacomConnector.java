package com.dionialves.core.connectors;

public class DatacomConnector extends DeviceConnector {

    public DatacomConnector(String username, String password) {
        super(username, password, "datacom");
        this.commandForBackup = "show running-config | nomore";
    }

}