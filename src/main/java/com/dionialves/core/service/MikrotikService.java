package com.dionialves.core.service;

public class MikrotikService extends DeviceService {


    public MikrotikService(String username, String password, int sshPort) {
        super(username, password, sshPort, "mikrotik");
        this.commandForBackup = "/export";
    }

    @Override
    protected boolean validateBackupContent(String content) {
        String contentLower = content.toLowerCase();

        if (content.length() < 100) {
            return false;
        }

        String[] validPatterns = {
                "/interface",
                "/ip address."
        };

        for (String pattern : validPatterns) {
            if (contentLower.contains(pattern.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
