package com.dionialves.core.service;

import java.util.Properties;

public class UbiquitiService extends DeviceService {

    public UbiquitiService(String username, String password, int sshPort) {
        super(username, password, sshPort, "ubiquiti");
        this.commandForBackup = "cat /tmp/system.cfg";
    }

    @Override
    protected Properties getConfig() {

        Properties config = new Properties();
        config.put("kex", "diffie-hellman-group-exchange-sha1,diffie-hellman-group14-sha1");
        config.put("server_host_key", "ssh-rsa,ssh-dss");
        config.put("pubkeyacceptedalgorithms", "ssh-rsa,ssh-dss");

        return config;
    }
}

