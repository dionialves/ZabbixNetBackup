package com.dionialves.zabbixnetbackup.strategy;

import com.dionialves.zabbixnetbackup.model.BackupResult;
import com.dionialves.zabbixnetbackup.model.Device;

public interface BackupStrategy {
    BackupResult execute(Device device);
}
