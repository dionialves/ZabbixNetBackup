package com.dionialves.core.connectors;

import com.dionialves.model.Device;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MimosaConnector {

    private static final String USER = "configure";

    private String password;

    private static final String LOGIN_PATH = "/login.php";
    private static final String DOWNLOAD_QUERY = "?q=preferences.configure&mimosa_action=download";

    public MimosaConnector(String password) {
        this.setPassword(password);
    }

    public void backupOfListDevices(List<Device> listOfDevices) throws IOException, InterruptedException {

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path folderBackup = Path.of("backup/mimosa", today);

        Files.createDirectories(folderBackup);

        for (Device device : listOfDevices) {
            createBackup(device, folderBackup);
        }
    }

    private void createBackup(Device device, Path folderBackup) throws IOException, InterruptedException {

        String baseUrl = "http://" + device.getIp();
        URI loginUri = URI.create(baseUrl + LOGIN_PATH);
        URI downloadUri = URI.create(baseUrl + DOWNLOAD_QUERY);

        Path arquivoSaida = folderBackup.resolve(device.getIp() + ".conf");

        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        String form = "username=" + USER + "&password=" + password;

        HttpRequest loginReq = HttpRequest.newBuilder()
                .uri(loginUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> loginResp = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
        int statusLogin = loginResp.statusCode();

        if (statusLogin == 200) {
            HttpRequest downloadReq = HttpRequest.newBuilder()
                    .uri(downloadUri)
                    .GET()
                    .build();

            HttpResponse<InputStream> downloadResp =
                    client.send(downloadReq, HttpResponse.BodyHandlers.ofInputStream());

            int statusDownload = downloadResp.statusCode();

            if (statusDownload == 200) {
                salvarArquivo(downloadResp.body(), arquivoSaida);
            }

            if (Files.exists(arquivoSaida) && Files.size(arquivoSaida) > 0) {
                System.out.println("SUCCESS: " + device.getIp());
            } else {
                System.out.println("FAILURE: " + device.getIp() + " - Arquivo vazio ou falha no download.");
                Files.deleteIfExists(arquivoSaida);
            }
        } else {
            System.out.println("FAILURE: " + device.getIp() + " - Status de login não foi 200 (retorno: " + statusLogin + ")");
        }
    }

    private void salvarArquivo(InputStream dados, Path destino) throws IOException {
        try (FileOutputStream out = new FileOutputStream(destino.toFile())) {
            dados.transferTo(out);
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password =  password.replace("&", "%26");
    }
}

