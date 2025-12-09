package com.dionialves.model;

public class Device {
    private final String ip;
    private Integer port;
    private final String manufacturer;

    public Device(String ip, Integer port, String manufacturer) {
        this.ip = ip;
        this.port = port;
        this.manufacturer = manufacturer;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }
}
