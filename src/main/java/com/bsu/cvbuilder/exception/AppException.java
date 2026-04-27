package com.bsu.cvbuilder.exception;

import lombok.Getter;
import lombok.Setter;

public class AppException extends RuntimeException {

    @Getter
    private final Integer statusCode;

    @Getter
    @Setter
    private Boolean skipped;

    public AppException(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public AppException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AppException(String message, Throwable cause, Integer statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public AppException(Throwable cause, Integer statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    public AppException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Integer statusCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.statusCode = statusCode;
    }
}
