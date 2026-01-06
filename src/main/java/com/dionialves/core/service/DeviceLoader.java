package com.dionialves.core.service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import com.dionialves.core.integration.ZabbixClient;
import com.dionialves.model.Device;

public class DeviceLoader {

    public static List<Device> loadDevices(String manufacturer, String groupId, int port) throws Exception {
        ZabbixClient zabbixApi = new ZabbixClient();
        zabbixApi.login();

        List<Map<String, String>> hostsList = zabbixApi.getHostsFromGroup(groupId);
        List<Device> listOfDevices = new ArrayList<>();

        for (Map<String, String> host : hostsList) {

                Device device = new Device(
                        host.get("ip"),
                        port,
                        manufacturer);
                listOfDevices.add(device);

        }

        System.out.println("\nZabbix data reading completed. "
                + listOfDevices.size()
                + " devices from manufacturer "
                + manufacturer
                + " loaded.");

        return listOfDevices;
    }
}
