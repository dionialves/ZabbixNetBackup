package com.dionialves.core.integration;

import com.dionialves.core.exception.ZabbixConnectionException;
import com.dionialves.core.exception.ZnbConfigException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ZabbixClient {
    private final String url;
    private final String username;
    private final String password;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private String authToken = null;

    private static final Path CONFIG_FILE = Path.of(System.getProperty("user.home"), ".znb", "config");

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
        if (authToken == null) {
            login();
        }

        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("method", method);
        req.add("params", params != null ? params : new JsonObject());
        req.addProperty("id", 1);
        req.addProperty("auth", authToken);

        try {
            return send(req);
        } catch (Exception e) {
            throw new ZabbixConnectionException(
                    "Zabbix API call failed: " + method, e
            );
        }
    }

    private JsonObject send(JsonObject req) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(req)))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return gson.fromJson(response.body(), JsonObject.class);
    }

    public List<Map<String, String>> getHostsFromGroup(String groupId) throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("output", "extend");
        params.addProperty("selectInterfaces", "extend");

        JsonArray groupIdsArray = new JsonArray();
        groupIdsArray.add(groupId);
        params.add("groupids", groupIdsArray);

        JsonObject response = call("host.get", params);

        JsonArray hosts = response.getAsJsonArray("result");
        List<Map<String, String>> lista = new ArrayList<>();


        if (hosts == null) return lista;

        for (JsonElement elem : hosts) {
            JsonObject hostObj = elem.getAsJsonObject();

            String hostid = hostObj.get("hostid").getAsString();
            String host = hostObj.get("host").getAsString();
            String name = hostObj.has("name") ? hostObj.get("name").getAsString() : "";

            // Extrai o primeiro IP
            String ip = "";
            if (hostObj.has("interfaces")) {
                JsonArray interfaces = hostObj.getAsJsonArray("interfaces");
                if (!interfaces.isEmpty()) {
                    JsonObject iface = interfaces.get(0).getAsJsonObject();
                    if (iface.has("ip")) {
                        ip = iface.get("ip").getAsString();
                    }
                }
            }

            Map<String, String> item = new HashMap<>();
            item.put("hostid", hostid);
            item.put("host", host);
            item.put("name", name);
            item.put("ip", ip);

            lista.add(item);
        }

        return lista;
    }

}
