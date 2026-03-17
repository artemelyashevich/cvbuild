package com.bsu.cvbuilder.web.controller.rest.exception;

import com.bsu.cvbuilder.domain.dto.exception.ExceptionBodyDto;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.exception.AuthTokenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalRestExceptionHandler {

    private static final String FAILED_VALIDATION_MESSAGE = "Validation failed.";
    private static final String UNEXPECTED_ERROR_MESSAGE = "Something went wrong.";

    @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
    public ResponseEntity<Map<String, String>> handleIncorrectResultSize(IncorrectResultSizeDataAccessException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Non unique result");
        response.put("details", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AuthTokenException.class)
    public ResponseEntity<Map<String, String>> handleAuthTokenException(AuthTokenException ex, HttpServletRequest request) {
        log.warn("PATH: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "message", ex.getMessage(),
                        "isExpired", ex.getIsExpired().toString()
                ));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ExceptionBodyDto> handleAppException(AppException e, HttpServletRequest request) {
        return ResponseEntity.status(e.getStatusCode()).body(handleException(e, null, request));
    }

    @SuppressWarnings("all")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionBodyDto> handleValidation(final MethodArgumentNotValidException exception, HttpServletRequest request) {
        var errors = exception.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                                FieldError::getField,
                                fieldError -> fieldError.getDefaultMessage(),
                                (exist, newMessage) -> exist + " " + newMessage
                        )
                );
        ExceptionBodyDto body = handleException(exception, FAILED_VALIDATION_MESSAGE, request);
        body.setErrors(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionBodyDto> handleException(final Exception exception, HttpServletRequest request) {
        log.error("ERROR: ", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(this.handleException(exception, UNEXPECTED_ERROR_MESSAGE, request));
    }

    private ExceptionBodyDto handleException(final Exception exception, final String defaultMessage, HttpServletRequest request) {
        var message = exception.getMessage() == null ? defaultMessage : exception.getMessage();
        var errorId = UUID.randomUUID();
        log.warn("PATH: {} --- '{}'.\nSee error details here: {}", request.getServletPath(), message, errorId);
        return new ExceptionBodyDto(message, Map.of("error", errorId.toString()));
    }
}
