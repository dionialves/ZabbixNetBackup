package com.dionialves.core.connectors;

import java.util.Properties;

public class CiscoSshSshConnector extends DeviceSshConnector {

    public CiscoSshSshConnector(String username, String password) {
        super(username, password, "cisco");
        this.commandForBackup = "show running-config";
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