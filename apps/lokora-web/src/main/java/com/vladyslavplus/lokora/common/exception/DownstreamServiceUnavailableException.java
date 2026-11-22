package com.vladyslavplus.lokora.common.exception;

public class DownstreamServiceUnavailableException extends RuntimeException {
    public DownstreamServiceUnavailableException(String serviceName) {
        super(serviceName + " is unavailable");
    }
}
