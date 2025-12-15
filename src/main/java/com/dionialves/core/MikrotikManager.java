package com.dionialves.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;


import com.dionialves.model.Device;

public class MikrotikManager {

    private final String username;
    private final String password;

    public MikrotikManager(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void backupDevices(List<Device> listOfDevices) throws IOException {

        String backupDirectory = this.createBackupDirectory();

        for (Device device: listOfDevices) {
            this.backupDevice(device, backupDirectory);
        }
    }

    private String createBackupDirectory() throws IOException {
        Path base = Path.of(System.getProperty("user.dir"), "backup", "mikrotik", LocalDate.now().toString());
        Files.createDirectories(base);
        return base.toString();
    }

    private void backupDevice(Device device, String backupDirectory) {
        String filename = device.getIp() + ".rsc";

        try (SSHClient ssh = this.connect(device)) {

            this.createFileBackup(ssh, filename);
            this.downloadBackup(ssh, filename, backupDirectory);
            this.deleteFileBackup(ssh, filename);

            System.out.println("SUCCESS: " + device.getIp());
        }
        catch (IOException e) {
            System.out.println("FAILURE: " + device.getIp() + " - " + e.getMessage());
        }
    }

    private SSHClient connect(Device device) throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        ssh.connect(device.getIp(), device.getPort());
        ssh.authPassword(this.username, this.password);

        return ssh;
    }

    private void createFileBackup(SSHClient ssh, String filename) throws IOException {
        executeCommand(ssh, "/export file=" + filename);
    }

    private void downloadBackup(SSHClient ssh, String filename, String backupDirectory) throws IOException {

        String remote = "/" + filename;
        String local = backupDirectory + "/" + filename;

        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.get(remote, local);
        }
    }

    private void deleteFileBackup(SSHClient ssh, String filename)  throws IOException  {
        executeCommand(ssh, "/file remove [find name=" + filename + "]");
    }

    private void executeCommand(SSHClient ssh, String command) throws IOException {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);
            cmd.join();
        }
    }
}