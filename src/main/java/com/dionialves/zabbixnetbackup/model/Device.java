package com.dionialves.zabbixnetbackup.model;

public class Device {
    private String host;
    private String vendor;
    private String username;
    private String password;
    private String backupFileExtension;
    private int port;
    private String backupCommand;

    public Device() {}

    public Device(String host, String username, String password, String vendor, int port, String backupFileExtension) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.vendor = vendor;
        this.port = port;
        this.backupFileExtension = backupFileExtension;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBackupFileExtension() {
        return backupFileExtension;
    }

    public void setBackupFileExtension(String backupFileExtension) {
        this.backupFileExtension = backupFileExtension;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getBackupCommand() {
        return backupCommand;
    }

    public void setBackupCommand(String backupCommand) {
        this.backupCommand = backupCommand;
    }
}
