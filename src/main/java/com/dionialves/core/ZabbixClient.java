package com.dionialves.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;


public class ZabbixClient {
    private final String url;
    private final String username;
    private final String password;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private String authToken = null;

    public ZabbixClient() {
        Dotenv dotenv = Dotenv.load();
        this.url = dotenv.get("ZABBIX_URL");
        this.username = dotenv.get("ZABBIX_USERNAME");
        this.password = dotenv.get("ZABBIX_PASSWORD");
    }

    public boolean login() throws Exception {
        JsonObject params = new JsonObject();
        params.addProperty("user", this.username);
        params.addProperty("password", this.password);

        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("method", "user.login");
        req.add("params", params);
        req.addProperty("id", 1);

        JsonObject res = send(req);

        if (res.has("result")) {
            authToken = res.get("result").getAsString();
            return true;
        }

        return false;
    }

    public JsonObject call(String method, JsonObject params) throws Exception {
        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("method", method);

        if (params == null)
            params = new JsonObject();

        req.add("params", params);
        req.addProperty("id", 1);

        if (authToken != null)
            req.addProperty("auth", authToken);

        return send(req);
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
