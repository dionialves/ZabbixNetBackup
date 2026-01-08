package com.dionialves.core.service;

import com.dionialves.model.BackupResult;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;

public class DigistarService extends DeviceService {
    private final String tftpUrl;

    public DigistarService(String username, String password, int sshPort, String tftpUrl) {
        super(username, password, sshPort, "digistar");

        this.backupFileExtension = ".tar";
        this.tftpUrl = tftpUrl;
    }

    @Override
    protected boolean validateBackupContent(String content) {
        return true;
    }

    @Override
    protected BackupResult backupDevice(String ip, String backupDir) throws JSchException {

        String data = LocalDate.now().format(DATE_FORMATTER);
        String filename = ip + "_" + data + backupFileExtension;

        try {
            Session session = this.connect(ip);

            if (!session.isConnected()) {
                String errorMsg = "Session not established";
                logger.warn("Failed to connect to {}: {}", ip, errorMsg);
                return BackupResult.failure(ip, errorMsg);
            }

            this.executeBackupViaTftp(session, filename);
            return BackupResult.success(ip);
        }
        catch (JSchException e) {
            String errorMsg = "SSH connection error: " + e.getMessage();
            logger.error("Error when backing up{}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
        catch (IOException e) {
            String errorMsg = "Erro de I/O: " + e.getMessage();
            logger.error("Error when backing up{}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
        catch (Exception e) {
            String errorMsg = "Unexpected error:" + e.getMessage();
            logger.error("Error when backing up {}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
    }

    private void executeBackupViaTftp(Session session, String filename) throws IOException, JSchException {

        ChannelShell channel = (ChannelShell) session.openChannel("shell");
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();

        channel.connect(5000);

        waitFor(in, ">", 5000);

        out.write("enable\n".getBytes());
        out.flush();

        waitFor(in, "Enter the programming password", 5000);

        out.write((this.password + "\n").getBytes());
        out.flush();

        waitFor(in, "#", 5000);

        out.write("terminal length 0\n".getBytes());
        out.flush();

        waitFor(in, "#", 3000);

        // Dump
        String backupCommand = "dump network " + this.tftpUrl + " " + filename + "\n";

        out.write(backupCommand.getBytes());
        out.flush();

        waitFor(in, "#", 40000);

        channel.disconnect();
    }


    private void waitFor(InputStream in, String expected, int timeoutMs)
            throws IOException {

        long start = System.currentTimeMillis();
        StringBuilder buffer = new StringBuilder();

        byte[] tmp = new byte[1024];

        while (System.currentTimeMillis() - start < timeoutMs) {
            while (in.available() > 0) {
                int len = in.read(tmp);
                if (len > 0) {
                    String chunk = new String(tmp, 0, len);
                    buffer.append(chunk);

                    if (buffer.toString().contains(expected)) {
                        return;
                    }
                }
            }
        }

        throw new IOException("Timeout esperando: " + expected);
    }
}
