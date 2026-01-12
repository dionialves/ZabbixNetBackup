package com.dionialves.zabbixnetbackup.service;

import com.dionialves.zabbixnetbackup.model.BackupResult;
import com.dionialves.zabbixnetbackup.model.Device;
import com.dionialves.zabbixnetbackup.strategy.BackupStrategy;
import com.dionialves.zabbixnetbackup.strategy.HttpDownloadBackupStrategy;
import com.dionialves.zabbixnetbackup.strategy.SSHPrintBackupStrategy;
import com.dionialves.zabbixnetbackup.strategy.SSHTftpBackupStrategy;

import java.util.HashMap;
import java.util.Map;

public class BackupService {

    private final Map<String, BackupStrategy> strategies;

    public BackupService() {
        strategies = new HashMap<>();
        strategies.put("cisco", new SSHPrintBackupStrategy());
        strategies.put("mikrotik", new SSHPrintBackupStrategy());
        strategies.put("ubiquiti", new SSHPrintBackupStrategy());
        strategies.put("digistar", new SSHTftpBackupStrategy());
        strategies.put("mimosa", new HttpDownloadBackupStrategy());
    }

    public BackupResult executeBackup(Device device) {
        BackupStrategy strategy = strategies.get(device.getVendor().toLowerCase());

        if (strategy == null) {
            String errorMsg = "Unsupported Device Type";
            return BackupResult.failure(device.getHost(), errorMsg);
        }

        return strategy.execute(device);
    }
}
