package com.dionialves.core;

import com.dionialves.model.Device;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class DatacomManager {

    private String username;
    private String password;

    public DatacomManager(String username, String password) {
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

    private void createBackup(Device device) throws IOException {
        String filename = device.getIp() + ".cfg";

        try {
            SSHClient ssh = this.connectAndLogin(device);

            Session session = ssh.startSession();
            Session.Command cmd = session.exec("show running-config | nomore");

            String output = IOUtils.readFully(cmd.getInputStream()).toString(StandardCharsets.UTF_8);

            String baseDir = System.getProperty("user.dir");
            String backupDir = baseDir + "/backup/Datacom";
            String backupTodayDir = backupDir + "/" + LocalDate.now();

            Files.createDirectories(Paths.get(backupDir));
            Files.createDirectories(Paths.get(backupTodayDir));

            String local = backupTodayDir + "/" + filename;

            Files.write(Paths.get(local), output.getBytes());

            session.close();
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
