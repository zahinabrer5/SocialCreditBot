package org.zahin.util;

public enum HttpResponseType {
    OK("OK", 200),
    NOT_FOUND("NOT_FOUND", 404);

    private final String status;
    private final int statusCode;

    HttpResponseType(String status, int statusCode) {
        this.status = status;
        this.statusCode = statusCode;
    }

    public String status() {
        return status;
    }

    public int statusCode() {
        return statusCode;
    }
}
