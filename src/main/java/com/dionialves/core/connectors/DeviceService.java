package com.dionialves.core.connectors;

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
    private boolean verbose = false;

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

        if (verbose) {
            System.out.println("Iniciando backup de " + devices.size() + " dispositivos...\n");
        }

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
            throw new UncheckedIOException("Erro ao criar diretório de backup: " + todayDir, e);
        }

        return todayDir;
    }

    protected BackupResult backupDevice(String ip, String backupDir) throws JSchException {
        String filename = ip + backupFileExtension;
        String filePath = backupDir + "/" + filename;

        try {
            Session session = this.connect(ip);

            if (!session.isConnected()) {
                String errorMsg = "Sessão não estabelecida";
                logger.warn("Falha ao conectar em {}: {}", ip, errorMsg);
                return BackupResult.failure(ip, errorMsg);
            }

            String config = readDeviceConfiguration(session);
            writeConfigToFile(config, filePath);

            logger.debug("Backup realizado com sucesso: {}", ip);
            return BackupResult.success(ip);
        }
        catch (JSchException e) {
            String errorMsg = "Erro de conexão SSH: " + e.getMessage();
            logger.error("Erro ao fazer backup de {}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
        catch (IOException e) {
            String errorMsg = "Erro de I/O: " + e.getMessage();
            logger.error("Erro ao fazer backup de {}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
        catch (Exception e) {
            String errorMsg = "Erro inesperado: " + e.getMessage();
            logger.error("Erro ao fazer backup de {}: {}", ip, errorMsg);
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

    protected void executeCommand(Session session, String command) throws JSchException, IOException {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");

            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            channel.connect();

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    protected void executeInteractiveCommands(Session session, List<String> commands) throws JSchException, IOException {
        ChannelShell channel = null;
        try {
            channel = (ChannelShell) session.openChannel("shell");

            InputStream input = channel.getInputStream();
            OutputStream output = channel.getOutputStream();

            channel.connect();

            for (String cmd : commands) {
                output.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
                output.flush();

                // Pequena espera para o equipamento responder
                Thread.sleep(500);
            }

            // Opcional: ler saída
            byte[] buffer = new byte[4096];
            while (input.available() > 0) {
                int bytesRead = input.read(buffer);
                if (bytesRead < 0) break;
                System.out.print(new String(buffer, 0, bytesRead));
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}

