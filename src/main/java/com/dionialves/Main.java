package com.dionialves;


import com.alibaba.fastjson.JSONObject;
import com.dionialves.cli.Menu;
import com.dionialves.core.ZabbixClient;
import com.google.gson.JsonObject;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.ZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    // --- VARIÁVEIS DE CONFIGURAÇÃO ---
    // !!! ATENÇÃO: SUBSTITUA ESTES VALORES PELOS SEUS !!!
    private static final String ZABBIX_URL = "http://zabbix.asngetel.net.br/zabbix/api_jsonrpc.php";
    private static final String ZABBIX_USER = "dioni.alves@getel.net.br";
    private static final String ZABBIX_PASSWORD = "ranaeu21";
    private static final String TARGET_GROUP_ID = "209"; // Exemplo: Substi

    public static void main(String[] args) throws Exception {

        Menu menu = new Menu();
        menu.dataEntry();

    }

}
