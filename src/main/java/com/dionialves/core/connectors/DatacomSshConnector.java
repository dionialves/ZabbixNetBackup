package com.dionialves.core.connectors;

public class DatacomSshConnector extends DeviceSshConnector {

    public DatacomSshConnector(String username, String password, int sshPort) {
        super(username, password, sshPort, "datacom");
        this.commandForBackup = "show running-config | nomore";
    }

}