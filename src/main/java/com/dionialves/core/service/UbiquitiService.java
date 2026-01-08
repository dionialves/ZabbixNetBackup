package com.dionialves.core.service;

import java.util.Properties;

public class UbiquitiService extends DeviceService {

    public UbiquitiService(String username, String password, int sshPort) {
        super(username, password, sshPort, "ubiquiti");
        this.commandForBackup = "cat /tmp/system.cfg";
    }

    @Override
    protected boolean validateBackupContent(String content) {
        String contentLower = content.toLowerCase();

        if (content.length() < 100) {
            return false;
        }

        String[] validPatterns = {
                "netconf.",
                "bridge.",
                "radio.",
                "wireless."
        };

        for (String pattern : validPatterns) {
            if (contentLower.contains(pattern.toLowerCase())) {
                return true;
            }
        }

        return false;
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

