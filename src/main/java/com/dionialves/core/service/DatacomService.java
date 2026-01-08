package com.dionialves.core.service;

public class DatacomService extends DeviceService {

    public DatacomService(String username, String password, int sshPort) {
        super(username, password, sshPort, "datacom");
        this.commandForBackup = "show running-config | nomore";
    }

}