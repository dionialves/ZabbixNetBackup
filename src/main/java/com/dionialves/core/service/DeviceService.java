package com.dionialves.core.service;

import com.dionialves.core.util.ProgressBar;
import com.dionialves.model.BackupResult;
import com.dionialves.model.BackupSummary;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class DeviceService {
    protected static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    protected final String username;
    protected final String password;
    protected final String vendor;
    protected final int port;
    protected String commandForBackup;

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static final int CONNECTION_TIMEOUT_MS = 10_000;
    protected String backupFileExtension;

    public DeviceService(String username, String password, int port, String vendor) {
        this.username = username;
        this.password = password;
        this.vendor = vendor;
        this.port = port;

        this.backupFileExtension = ".cfg";
    }

    public BackupSummary backupDevices(List<Map<String, String>> devices) throws JSchException, IOException {

        String backupDir = this.createBackupDirectory(this.vendor);
        BackupSummary summary = new BackupSummary();

        ProgressBar progressBar = new ProgressBar("Backup", devices.size());

        for (Map<String, String> device : devices) {
            BackupResult result = this.backupDevice(device.get("ip"), backupDir);
            summary.add(result);

            progressBar.setExtraMessage(device.get("ip"));
            progressBar.step();
        }

        progressBar.close();

        summary.finish();
        return summary;
    }

    private String createBackupDirectory(String vendor) throws IOException {
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

    protected BackupResult backupDevice(String ip, String backupDir) throws JSchException {
        String filename = ip + backupFileExtension;
        String filePath = backupDir + "/" + filename;

        try {
            Session session = this.connect(ip);

            if (!session.isConnected()) {
                String errorMsg = "Session not established";
                logger.warn("Failed to connect to {}: {}", ip, errorMsg);
                return BackupResult.failure(ip, errorMsg);
            }

            String config = readDeviceConfiguration(session);
            writeConfigToFile(config, filePath);

            logger.debug("Backup performed successfully: {}", ip);
            return BackupResult.success(ip);
        }
        catch (JSchException e) {
            String errorMsg = "SSH connection error:" + e.getMessage();
            logger.error("Error when backing up {}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
        catch (IOException e) {
            String errorMsg = "I/O error: " + e.getMessage();
            logger.error("Error when backing up {}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
        catch (Exception e) {
            String errorMsg = "Unexpected error: " + e.getMessage();
            logger.error("Error when backing up{}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
    }

    protected Session connect(String ip) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, ip, this.port);
        session.setPassword(password);

        session.setConfig("StrictHostKeyChecking", "no");

        session.setConfig(this.getConfig());
        try {
            session.connect(CONNECTION_TIMEOUT_MS);
        } catch (JSchException e) {
            session.disconnect();
        }

        return session;
    }

    protected Properties getConfig() {
        return new Properties();
    }

    protected String readDeviceConfiguration(Session session) throws JSchException, IOException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");

            channel.setCommand(this.commandForBackup);
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
        Files.createDirectories(Paths.get(filePath).getParent());
        Files.writeString(Paths.get(filePath), config);
    }
}

