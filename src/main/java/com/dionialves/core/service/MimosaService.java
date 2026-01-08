package com.dionialves.core.service;

import com.dionialves.core.util.ProgressBar;
import com.dionialves.model.BackupResult;
import com.dionialves.model.BackupSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class MimosaService {
    private static final Logger logger = LoggerFactory.getLogger(MimosaService.class);

    private final String username;
    private final String password;
    private final int port;
    private final String vendor;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String LOGIN_PATH = "/login.php";
    private static final String DOWNLOAD_QUERY = "?q=preferences.configure&mimosa_action=download";

    public MimosaService(String password, int port) {
        this.password = setPassword(password);
        this.port = port;
        this.username = "configure";
        this.vendor = "mimosa";
    }

    public BackupSummary backupDevices(List<Map<String, String>> devices) throws IOException, InterruptedException {

        BackupSummary summary = new BackupSummary();
        Path backupDir = this.createBackupDirectory(this.vendor);

        ProgressBar progressBar = new ProgressBar("Backup Mimosa", devices.size());

        for (Map<String, String> device : devices) {
            String ip = device.get("ip");

            BackupResult result = this.backupDevice(ip, backupDir);
            summary.add(result);


            progressBar.setExtraMessage(ip);
            progressBar.step();
        }

        progressBar.close();

        summary.finish();
        return summary;

    }

    private Path createBackupDirectory(String vendor) {
        Path todayDir = Path.of(
                System.getProperty("user.dir"),
                "backup",
                vendor,
                LocalDate.now().format(DATE_FORMATTER)
        );

        try {
            Files.createDirectories(todayDir);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Error creating backup directory: " + todayDir, e
            );
        }

        return todayDir;
    }

    private BackupResult backupDevice(String ip, Path backupFolder) throws IOException, InterruptedException {
        Path outputFile = backupFolder.resolve(ip + ".conf");

        try {
            HttpClient client = createHttpClient();
            String baseUrl = "http://" + ip + ":" + this.port;

            if (authenticate(client, baseUrl)) {
                downloadAndSaveBackup(client, baseUrl, outputFile, ip);

                // Validar se o arquivo foi salvo corretamente
                if (Files.exists(outputFile) && Files.size(outputFile) > 0) {
                    return BackupResult.success(ip);
                } else {
                    String errorMsg = "Empty file or download failed";
                    logger.warn("Backup failed {}: {}", ip, errorMsg);
                    Files.deleteIfExists(outputFile);
                    return BackupResult.failure(ip, errorMsg);
                }
            } else {
                String errorMsg = "Authentication failed";
                logger.warn("Failed to authenticate to {}", ip);
                return BackupResult.failure(ip, errorMsg);
            }

        } catch (IOException e) {
            String errorMsg = "I/O error: " + e.getMessage();
            logger.error("Error when backing up {}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        } catch (InterruptedException e) {
            String errorMsg = "Operation interrupted: " + e.getMessage();
            logger.error("Backup stopped for {}: {}", ip, errorMsg);
            Thread.currentThread().interrupt();
            return BackupResult.failure(ip, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Unexpected error: " + e.getMessage();
            logger.error("Unexpected error when backing up {}: {}", ip, errorMsg);
            return BackupResult.failure(ip, errorMsg);
        }
    }

    private HttpClient createHttpClient() {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        return HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private boolean authenticate(HttpClient client, String baseUrl) throws IOException, InterruptedException {
        URI loginUri = URI.create(baseUrl + LOGIN_PATH);
        String formData = "username=" + this.username + "&password=" + this.password;

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(loginUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        return loginResponse.statusCode() == 200;
    }

    private void downloadAndSaveBackup(HttpClient client, String baseUrl, Path outputFile, String deviceIp)
            throws IOException, InterruptedException {
        URI downloadUri = URI.create(baseUrl + DOWNLOAD_QUERY);

        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(downloadUri)
                .GET()
                .build();

        HttpResponse<InputStream> downloadResponse = client.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream());

        if (downloadResponse.statusCode() == 200) {
            saveFile(downloadResponse.body(), outputFile);
        } else {
            throw new IOException("Download failed - Status: " + downloadResponse.statusCode());
        }
    }

    private void saveFile(InputStream dados, Path destino) throws IOException {
        try (FileOutputStream out = new FileOutputStream(destino.toFile())) {
            dados.transferTo(out);
        }
    }

    public String setPassword(String password) {
        return password.replace("&", "%26");
    }
}
