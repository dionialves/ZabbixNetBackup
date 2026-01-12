package com.dionialves.zabbixnetbackup.integration.zabbix;

import com.dionialves.core.exception.ZabbixConnectionException;
import com.dionialves.core.exception.ZnbConfigException;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ZabbixClient {
    private final String url;
    private final String username;
    private final String password;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private String authToken = null;

    private static final Path CONFIG_FILE = Path.of(System.getProperty("user.home"), ".znb", "config");
    private static final Logger logger = LoggerFactory.getLogger(ZabbixClient.class);


    public ZabbixClient() throws IOException {
        Properties props = loadConfig();
        this.url = getRequired(props, "ZABBIX_URL");
        this.username = getRequired(props, "ZABBIX_USERNAME");
        this.password = getRequired(props, "ZABBIX_PASSWORD");
    }

    private Properties loadConfig() {
        if (!Files.exists(CONFIG_FILE)) {
            throw new ZnbConfigException(
                    "ZNB is not initialized.\nRun 'znb init' first."
            );
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            throw new ZnbConfigException(
                    "Failed to read ZNB configuration file."
            );
        }
        return props;
    }

    private String getRequired(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ZnbConfigException(
                    "Missing required configuration: " + key
            );
        }
        return value;
    }

    public void login() throws Exception {
        try {
            JsonObject params = new JsonObject();
            params.addProperty("user", username);
            params.addProperty("password", password);

            JsonObject req = new JsonObject();
            req.addProperty("jsonrpc", "2.0");
            req.addProperty("method", "user.login");
            req.add("params", params);
            req.addProperty("id", 1);

            JsonObject res = send(req);

            if (!res.has("result")) {
                throw new ZabbixConnectionException(
                        "Failed to authenticate with Zabbix API.", null
                );
            }

            authToken = res.get("result").getAsString();

        } catch (Exception e) {
            throw new ZabbixConnectionException(
                    "Unable to connect to Zabbix API.", e
            );
        }
    }

    public JsonObject call(String method, JsonObject params) throws Exception {
        try {
            if (authToken == null) {
                login();
            }

            return callWithAuth(method, params);
        } catch (Exception e) {
            throw new ZabbixConnectionException("Zabbix API call failed: " + method, e);
        }
    }

    private JsonObject callWithAuth(String method, JsonObject params) throws Exception {
        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("method", method);
        req.add("params", params != null ? params : new JsonObject());
        req.addProperty("id", 1);
        req.addProperty("auth", authToken);

        return send(req);
    }

    private JsonObject send(JsonObject req) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(req)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return gson.fromJson(response.body(), JsonObject.class);
    }

    public List<ZabbixHostDTO> getHostsFromGroup(String groupId){

        if (groupId == null || groupId.trim().isEmpty()) {
            throw new IllegalArgumentException("groupId cannot be null or empty");
        }

        try {
            JsonArray hostsJson = this.getHostsByJson(groupId);
            return this.hostJsonToDto(hostsJson);
        } catch (ZabbixConnectionException e) {
            throw new ZabbixConnectionException("Unable to connect to Zabbix API.", e);
        } catch (Exception e) {
            logger.error("Error fetching hosts from group {}", groupId, e);
            return List.of();
        }
    }

    private JsonArray getHostsByJson(String groupId) throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("output", "extend");
        params.addProperty("selectInterfaces", "extend");

        JsonArray groupIdsArray = new JsonArray();
        groupIdsArray.add(groupId);
        params.add("groupids", groupIdsArray);

        JsonObject response = call("host.get", params);

        JsonArray result = response.getAsJsonArray("result");
        return result != null ? result : new JsonArray();
    }

    private List<ZabbixHostDTO> hostJsonToDto(JsonArray hostsJson) {
        return StreamSupport.stream(hostsJson.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(this::mapToHostDTO)
                .collect(Collectors.toList());
    }

    private ZabbixHostDTO mapToHostDTO(JsonObject hostJson) {
        String hostId = hostJson.has("hostid") ? hostJson.get("hostid").getAsString() : "";
        String name = hostJson.has("name") ? hostJson.get("name").getAsString() : "";
        String ip = extractFirstIpAddress(hostJson);

        return new ZabbixHostDTO(hostId, name, ip);
    }

    private String extractFirstIpAddress(JsonObject hostJson) {
        if (!hostJson.has("interfaces")) {
            return "";
        }

        JsonArray interfaces = hostJson.getAsJsonArray("interfaces");
        if (interfaces.isEmpty()) {
            return "";
        }

        JsonObject firstInterface = interfaces.get(0).getAsJsonObject();
        return firstInterface.has("ip") ? firstInterface.get("ip").getAsString() : "";
    }
}
