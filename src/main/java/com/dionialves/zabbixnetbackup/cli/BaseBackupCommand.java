package com.dionialves.zabbixnetbackup.cli;

import com.dionialves.zabbixnetbackup.integration.zabbix.ZabbixHostDTO;
import com.dionialves.zabbixnetbackup.model.BackupResult;
import com.dionialves.zabbixnetbackup.integration.zabbix.ZabbixClient;
import com.dionialves.zabbixnetbackup.service.BackupService;
import com.dionialves.zabbixnetbackup.util.ProgressBar;
import com.dionialves.zabbixnetbackup.model.Device;
import com.dionialves.zabbixnetbackup.model.BackupSummary;

import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseBackupCommand implements Runnable {

    @Option(
            names = {"-u", "--username"},
            required = true,
            description = "Device username"
    )
    protected String username;

    @Option(
            names = {"-p", "--password"},
            required = true,
            interactive = true,
            description = "Device password"
    )
    protected String password;

    @Option(
            names = {"-g", "--group-id"},
            required = true,
            description = "Zabbix host group ID"
    )
    protected String groupId;

    protected abstract String getVendorName();
    protected abstract int getPort();
    protected abstract String getBackupCommand();

    @Override
    public void run() {
        try {
            List<Device> devices = getDevices();
            executeBackup(devices);
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("\nTip: Run 'znb init' to configure Zabbix credentials.");
        } catch (Exception e) {
            System.err.println("\nUnexpected error: " + e.getMessage());
        }
    }

    private List<Device> getDevices() throws Exception {
        List<Device> devices = new ArrayList<>();
        ZabbixClient zabbix = new ZabbixClient();

        List<ZabbixHostDTO> hosts = zabbix.getHostsFromGroup(groupId);

        if (hosts.isEmpty()) {
            System.err.println("No devices found in group " + groupId);
            return devices;
        }

        for (ZabbixHostDTO host : hosts) {
            devices.add(createDevice(host));
        }
        return devices;
    }

    protected Device createDevice(ZabbixHostDTO host) {
        return new Device(
                host.getIp(),
                username,
                password,
                getVendorName(),
                getPort(),
                getBackupCommand()
        );
    }

    private void executeBackup(List<Device> devices) {
        ProgressBar progressBar = new ProgressBar("Backup", devices.size());
        BackupSummary summary = new BackupSummary();
        BackupService backupService = new BackupService();

        for (Device device : devices) {
            BackupResult result = backupService.executeBackup(device);
            summary.add(result);

            progressBar.setExtraMessage(device.getHost());
            progressBar.step();
        }

        progressBar.close();
        summary.finish();
    }
}