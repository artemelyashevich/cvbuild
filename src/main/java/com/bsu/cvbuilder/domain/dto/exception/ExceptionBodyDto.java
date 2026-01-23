package com.bsu.cvbuilder.domain.dto.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionBodyDto {

    private String message;
    private Map<String, String> errors;

    public ExceptionBodyDto(String message) {
        this.message = message;
        this.errors = new HashMap<>();
    }
}