package org.community.privacy.infrai;

import java.util.Map;

public final class InfraiException extends RuntimeException {
    private final String code;
    private final int statusCode;

    public InfraiException(String code, Map<String, Object> error, int statusCode) {
        super(String.valueOf(error.getOrDefault("message", code)));
        this.code = code;
        this.statusCode = statusCode;
    }

    public String code() {
        return code;
    }

    public int statusCode() {
        return statusCode;
    }
}
