package com.bsu.cvbuilder.web.controller.rest.exception;

import com.bsu.cvbuilder.exception.AppException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, String>> handleAppException(AppException e) {
        return ResponseEntity.status(e.getStatusCode()).body(
                Map.of("message", e.getMessage())
        );
    }
}
