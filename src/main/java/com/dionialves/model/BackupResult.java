package com.dionialves.model;

import java.time.LocalDateTime;

public class BackupResult {
    private final String ip;
    private final boolean success;
    private final String message;
    private final LocalDateTime timestamp;

    private BackupResult(String ip, boolean success, String message) {
        this.ip = ip;
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public static BackupResult success(String ip) {
        return new BackupResult(ip, true, null);
    }

    public static BackupResult failure(String ip, String message) {
        return new BackupResult(ip, false, message);
    }

    public String getIp() {
        return ip;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}