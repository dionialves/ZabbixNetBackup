package com.dionialves.core.exception;

public class ZabbixConnectionException extends RuntimeException {
    public ZabbixConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
