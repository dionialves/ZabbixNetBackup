package com.dionialves.core;

import com.dionialves.model.Device;

import com.jcraft.jsch.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class CiscoManager {

    private String username;
    private String password;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int CONNECTION_TIMEOUT_MS = 10_000;
    private static final int CHANNEL_CONNECT_TIMEOUT_MS = 5_000;

    public CiscoManager(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void backupDevices(List<Device> devices) throws JSchException, IOException {

        String backupDir = this.createBackupDirectory();

        for (Device device : devices) {
            this.backupDevice(device, backupDir);
        }
    }

    private String createBackupDirectory() throws IOException {
        String baseDir = System.getProperty("user.dir");
        String backupRoot = Paths.get(baseDir, "backup", "cisco").toString();
        String todayDir = Paths.get(backupRoot, LocalDate.now().format(DATE_FORMATTER)).toString();

        try {
            Files.createDirectories(Paths.get(todayDir));
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao criar diretório de backup: " + todayDir, e);
        }

        return todayDir;
    }

    private void backupDevice(Device device, String backupDir) throws JSchException {
        String filename = device.getIp() + ".cfg";
        String filePath = backupDir + "/" + filename;

        try {
            Session session = this.connect(device);
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
        Properties config = new java.util.Properties();
        config.put("kex", "diffie-hellman-group-exchange-sha1,diffie-hellman-group14-sha1");
        config.put("server_host_key", "ssh-rsa,ssh-dss");
        config.put("pubkeyacceptedalgorithms", "ssh-rsa,ssh-dss");

        session.setConfig(config);
        session.connect(CONNECTION_TIMEOUT_MS);

        return session;
    }

    private String fetchRunningConfig(Session session) throws JSchException, IOException {
        ChannelShell channel = null;
        try {
            channel = (ChannelShell) session.openChannel("shell");
            channel.connect(CHANNEL_CONNECT_TIMEOUT_MS);

            try (OutputStream out = channel.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(out), true);
                 InputStream in = channel.getInputStream();
                 ByteArrayOutputStream response = new ByteArrayOutputStream()) {

                writer.println("terminal length 0");
                writer.println("show running-config");
                writer.println("exit");

                byte[] buffer = new byte[8192];
                long timeout = System.currentTimeMillis() + 15_000;

                while (System.currentTimeMillis() < timeout) {
                    while (in.available() > 0) {
                        int read = in.read(buffer);
                        if (read < 0) break;
                        response.write(buffer, 0, read);
                    }

                    if (channel.isClosed()) {
                        while (in.available() > 0) {
                            int read = in.read(buffer);
                            if (read < 0) break;
                            response.write(buffer, 0, read);
                        }
                        break;
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                String output = response.toString("UTF-8");

                return cleanOutput(output);
            }
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private String cleanOutput(String output) {
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
