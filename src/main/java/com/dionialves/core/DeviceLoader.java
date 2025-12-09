package com.dionialves.core;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import com.dionialves.model.Device;

public class DeviceLoader {

    public static List<Device> loadDevices(String manufacturer, String groupId) throws Exception {
        ZabbixClient zabbixApi = new ZabbixClient();
        zabbixApi.login();

        List<Map<String, String>> hostsList = zabbixApi.getHostsFromGroup(groupId);
        List<Device> listOfDevices = new ArrayList<>();

        int port = 22;
        if (Objects.equals(manufacturer, "Mikrotik")) {
            port = 2300;
        }

        for (Map<String, String> host : hostsList) {

                Device device = new Device(
                        host.get("ip"),
                        port,
                        manufacturer);
                listOfDevices.add(device);

        }

        System.out.println("✅ Leitura de arquivo concluída. " + listOfDevices.size() + " equipamentos do tipo " + manufacturer + " carregados.");

        return listOfDevices;
    }
}
