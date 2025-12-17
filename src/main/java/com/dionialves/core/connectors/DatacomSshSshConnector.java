package com.dionialves.core.connectors;

public class DatacomSshSshConnector extends DeviceSshConnector {

    public DatacomSshSshConnector(String username, String password) {
        super(username, password, "datacom");
        this.commandForBackup = "show running-config | nomore";
    }

}