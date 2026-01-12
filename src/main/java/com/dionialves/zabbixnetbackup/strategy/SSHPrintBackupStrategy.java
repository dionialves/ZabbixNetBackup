package com.dionialves.zabbixnetbackup.strategy;

import com.dionialves.zabbixnetbackup.model.BackupResult;
import com.dionialves.zabbixnetbackup.model.Device;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SSHPrintBackupStrategy implements BackupStrategy {
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public BackupResult execute(Device device) {
        String backupDir = this.createBackupDirectory(device.getVendor());

        String filename = device.getHost() + device.getBackupCommand();
        String filePath = backupDir + "/" + filename;

        try {
            Session session = this.connect(device);

            if (!session.isConnected()) {
                String errorMsg = "Session not established";
                return BackupResult.failure(device.getHost(), errorMsg);
            }

            String config = this.readDeviceConfiguration(session, device);

            this.writeConfigToFile(config, filePath);
            return BackupResult.success(device.getHost());

        }
        catch (JSchException e) {
            String errorMsg = "SSH connection error:" + e.getMessage();
            return BackupResult.failure(device.getHost(), errorMsg);
        }
        catch (IOException e) {
            String errorMsg = "I/O error: " + e.getMessage();
            return BackupResult.failure(device.getHost(), errorMsg);
        }
        catch (Exception e) {
            String errorMsg = "Unexpected error: " + e.getMessage();
            return BackupResult.failure(device.getHost(), errorMsg);
        }
    }

    protected Session connect(Device device) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(device.getUsername(), device.getHost(), device.getPort());
        session.setPassword(device.getPassword());

        session.setConfig("StrictHostKeyChecking", "no");

        try {
            session.connect(10_000);
        } catch (JSchException e) {
            session.disconnect();
        }

        return session;
    }

    protected String readDeviceConfiguration(Session session, Device device) throws JSchException, IOException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");

            channel.setCommand(device.getBackupCommand());
            channel.setInputStream(null);
            InputStream inputStream = channel.getInputStream();
            channel.connect();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    private void writeConfigToFile(String config, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, config);
    }

    private String createBackupDirectory(String vendor) {
        String baseDir = System.getProperty("user.dir");
        String backupRoot = Paths.get(baseDir, "backup", vendor).toString();
        String todayDir = Paths.get(backupRoot, LocalDate.now().format(DATE_FORMATTER)).toString();

        try {
            Files.createDirectories(Paths.get(todayDir));
        } catch (IOException e) {
            throw new UncheckedIOException("Error creating backup directory: " + todayDir, e);
        }

        return todayDir;
    }
}
