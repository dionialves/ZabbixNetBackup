package com.dionialves.core.connectors;

import com.dionialves.model.Device;
import com.jcraft.jsch.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public abstract class DeviceConnector {
    protected final String username;
    protected final String password;
    protected final String vendor;
    protected String commandForBackup;

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static final int CONNECTION_TIMEOUT_MS = 10_000;
    protected static final int CHANNEL_CONNECT_TIMEOUT_MS = 5_000;
    protected static final String BACKUP_FILE_EXTENSION = ".cfg";


    public DeviceConnector(String username, String password, String vendor) {
        this.username = username;
        this.password = password;
        this.vendor = vendor;
    }

    public void backupDevices(List<Device> devices) throws JSchException, IOException {

        String backupDir = this.createBackupDirectory(this.vendor);

        for (Device device : devices) {
            this.backupDevice(device, backupDir);
        }
    }

    private String createBackupDirectory(String vendor) throws IOException {
        String baseDir = System.getProperty("user.dir");
        String backupRoot = Paths.get(baseDir, "backup", vendor).toString();
        String todayDir = Paths.get(backupRoot, LocalDate.now().format(DATE_FORMATTER)).toString();

        try {
            Files.createDirectories(Paths.get(todayDir));
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao criar diretório de backup: " + todayDir, e);
        }

        return todayDir;
    }

    protected void backupDevice(Device device, String backupDir) throws JSchException {
        String filename = device.getIp() + BACKUP_FILE_EXTENSION;
        String filePath = backupDir + "/" + filename;

        try {
            Session session = this.connect(device);

            if (!session.isConnected()) {
                System.out.println("FAILURE: " + device.getIp() + " - Session could not be established.");
                return;
            }

            String config = fetchRunningConfig(session);
            writeConfigToFile(config, filePath);

            System.out.println("SUCCESS: " + device.getIp());
        }
        catch (IOException e) {
            System.out.println("FAILURE: " + device.getIp() + " - " + e.getMessage());
        }
    }

    private Session connect(Device device) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, device.getIp(), device.getPort());
        session.setPassword(password);

        session.setConfig("StrictHostKeyChecking", "no");

        session.setConfig(this.getConfig());
        try {
            session.connect(CONNECTION_TIMEOUT_MS);
        } catch (JSchException e) {
            System.out.println("FAILURE: " + device.getIp() + " - " + e.getMessage());
            session.disconnect();
        }

        return session;
    }

    protected Properties getConfig() {
        return new Properties();
    }

    protected String fetchRunningConfig(Session session) throws JSchException, IOException {
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

    protected String cleanOutput(String output) {
        String[] lines = output.split("\n");
        StringBuilder cleaned = new StringBuilder();

        boolean started = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("show running-config") || trimmed.startsWith("terminal length 0") || trimmed.startsWith("exit")) {
                continue;
            }
            if (trimmed.contains("Building configuration") || trimmed.startsWith("Current configuration") || trimmed.startsWith("!")) {
                started = true;
            }
            if (started) {
                cleaned.append(line).append("\n");
            }
        }
        return cleaned.toString();
    }

    private void writeConfigToFile(String config, String filePath) throws IOException {
        Files.createDirectories(Paths.get(filePath).getParent());
        Files.writeString(Paths.get(filePath), config);
    }
}

