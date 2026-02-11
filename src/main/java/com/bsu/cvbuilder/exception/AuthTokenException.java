package com.bsu.cvbuilder.exception;

import lombok.Getter;

@Getter
public class AuthTokenException extends RuntimeException {

    private final Boolean isExpired;

    public AuthTokenException(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public AuthTokenException(String message, Boolean isExpired) {
        super(message);
        this.isExpired = isExpired;
    }

    public AuthTokenException(String message, Throwable cause, Boolean isExpired) {
        super(message, cause);
        this.isExpired = isExpired;
    }

    public AuthTokenException(Throwable cause, Boolean isExpired) {
        super(cause);
        this.isExpired = isExpired;
    }

    public AuthTokenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Boolean isExpired) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.isExpired = isExpired;
    }
}
