package com.dionialves.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;


import com.dionialves.model.Device;

public class MikrotikManager {

    private String username;
    private String password;

    public MikrotikManager(String username, String password) {
        this.username = username;
        this.password = password;
    }

    private SSHClient connectAndLogin(Device device) throws IOException {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());


        ssh.connect(device.getIp(), device.getPort());
        ssh.authPassword(this.username, this.password);

        return ssh;

    }

    private void createFileBackup(SSHClient ssh, String filename) throws IOException {

        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec("/export file=" + filename);
            cmd.join();
        }

    }

    private void downloadBackup(SSHClient ssh, String filename) throws IOException {
        String baseDir = System.getProperty("user.dir");
        String backupDir = baseDir + "/backup/Mikrotik";
        String backupTodayDir = backupDir + "/" + LocalDate.now();

        Files.createDirectories(Paths.get(backupDir));
        Files.createDirectories(Paths.get(backupTodayDir));

        String remote = "/" + filename;
        String local = backupTodayDir + "/" + filename;

        try (SFTPClient sftp = ssh.newSFTPClient()) {
            sftp.get(remote, local);
        }
    }

    private void deleteFileBackup(SSHClient ssh, String filename)  throws IOException  {
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec("/file remove [find name=" + filename + "]");
            cmd.join();
        }
    }

    private void createBackup(Device device) throws IOException {
        String filename = device.getIp() + ".rsc";

        try {
            SSHClient ssh = this.connectAndLogin(device);
            this.createFileBackup(ssh, filename);
            this.downloadBackup(ssh, filename);
            this.deleteFileBackup(ssh, filename);

            ssh.disconnect();

            System.out.println("SUCCESS: " + device.getIp());
        }
        catch (IOException e) {
            System.out.println("FAILURE: " + device.getIp() + " - " + e.getMessage());
        }
    }

    public void backupOfListDevices(List<Device> listOfDevices) throws IOException {

        for (Device device: listOfDevices) {
            this.createBackup(device);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
