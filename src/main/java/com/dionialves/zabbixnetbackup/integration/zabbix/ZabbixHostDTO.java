package com.dionialves.zabbixnetbackup.integration.zabbix;

public class ZabbixHostDTO {
    private String id;
    private String hostname;
    private String ip;

    public ZabbixHostDTO(String id, String hostname, String ip) {
        this.id = id;
        this.hostname = hostname;
        this.ip = ip;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
