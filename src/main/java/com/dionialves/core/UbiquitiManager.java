package com.dionialves.core;

import com.dionialves.model.Device;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class UbiquitiManager {
    private String username;
    private String password;

    public UbiquitiManager(String username, String password) {
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

    private void downloadBackup(SSHClient ssh, String filename) throws IOException {
        String baseDir = System.getProperty("user.dir");
        String backupDir = baseDir + "/backup/Ubiquit";
        String backupTodayDir = backupDir + "/" + LocalDate.now();

        Files.createDirectories(Paths.get(backupDir));
        Files.createDirectories(Paths.get(backupTodayDir));

        String remote = "/tmp/system.cfg";
        String local = backupTodayDir + "/" + filename + ".cfg";

        SCPFileTransfer scp = ssh.newSCPFileTransfer();
        scp.download(remote, local);

    }

    private void createBackup(Device device) throws IOException {
        String filename = device.getIp();

        try {
            SSHClient ssh = this.connectAndLogin(device);
            this.downloadBackup(ssh, filename);

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
