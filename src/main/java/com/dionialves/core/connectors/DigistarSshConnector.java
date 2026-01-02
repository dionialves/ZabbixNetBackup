package com.dionialves.core.connectors;

import com.dionialves.model.Device;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;

public class DigistarSshConnector extends DeviceSshConnector {
    private final String tftpUrl;

    public DigistarSshConnector(String username, String password, String vendor) {
        super(username, password, vendor);

        this.backupFileExtension = ".tar";

        Dotenv dotenv = Dotenv.load();
        this.tftpUrl = dotenv.get("TFTP_SERVER");
    }

    @Override
    protected void backupDevice(Device device, String backupDir) throws JSchException {
        String data = LocalDate.now().format(DATE_FORMATTER);
        String filename = device.getIp() + "_" + data + backupFileExtension;

        Session session = null;
        try {
            session = this.connect(device);

            if (!session.isConnected()) {
                System.out.println("FAILURE: " + device.getIp() + " - Session could not be established.");
                return;
            }

            this.executeBackupViaTftp(session, filename);

            System.out.println("SUCCESS: " + device.getIp());
        }
        catch (IOException e) {
            System.out.println("FAILURE: " + device.getIp() + " - " + e.getMessage());
        }
        finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
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
