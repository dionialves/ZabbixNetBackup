package com.dionialves.core.connectors;

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

public class MimosaHttpConnector {
    private final String username;
    private final String password;
    private final int port;
    private final String vendor;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String LOGIN_PATH = "/login.php";
    private static final String DOWNLOAD_QUERY = "?q=preferences.configure&mimosa_action=download";

    public MimosaHttpConnector(String password, int port) {
        this.password = setPassword(password);
        this.port = port;
        this.username = "configure";
        this.vendor = "mimosa";
    }

    public void backupDevices(List<Map<String, String>> devices) throws IOException, InterruptedException {

        Path backupDir = this.createBackupDirectory(this.vendor);

        for (Map<String, String> device : devices) {
            this.backupDevice(device.get("ip"), backupDir);
        }
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
                    "Erro ao criar diretório de backup: " + todayDir, e
            );
        }

        return todayDir;
    }

    private void backupDevice(String ip, Path backupFolder) throws IOException, InterruptedException {
        Path outputFile = backupFolder.resolve(ip + ".conf");

        HttpClient client = createHttpClient();
        String baseUrl = "http://" + ip + ":" + this.port;

        if (authenticate(client, baseUrl)) {
            downloadAndSaveBackup(client, baseUrl, outputFile, ip);
        } else {
            logFailure(ip, "Authentication failed");
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
            validateAndLogResult(outputFile, deviceIp);
        } else {
            logFailure(deviceIp, "Download failed - Status: " + downloadResponse.statusCode());
        }
    }

    private void validateAndLogResult(Path outputFile, String deviceIp) throws IOException {
        if (Files.exists(outputFile) && Files.size(outputFile) > 0) {
            logSuccess(deviceIp);
        } else {
            logFailure(deviceIp, "Empty file or download failed");
            Files.deleteIfExists(outputFile);
        }
    }

    private void saveFile(InputStream dados, Path destino) throws IOException {
        try (FileOutputStream out = new FileOutputStream(destino.toFile())) {
            dados.transferTo(out);
        }
    }

    // Responsibility: Log success
    private void logSuccess(String deviceIp) {
        System.out.println("SUCCESS: " + deviceIp);
    }

    // Responsibility: Log failure
    private void logFailure(String deviceIp, String reason) {
        System.out.println("FAILURE: " + deviceIp + " - " + reason);
    }

    public String setPassword(String password) {
        return password.replace("&", "%26");
    }
}
