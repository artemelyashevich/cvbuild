package com.bsu.cvbuilder.service.flow.form.validator;

import java.util.regex.Pattern;

public class RegexValidator implements FieldValidator {

    private final Pattern pattern;
    private final String message;

    public RegexValidator(String regex, String message) {
        this.pattern = Pattern.compile(regex);
        this.message = message;
    }

    @Override
    public ValidationResult validate(String value) {

        if (value == null || value.isBlank()) {
            return ValidationResult.ok();
        }

        if (!pattern.matcher(value).matches()) {
            return ValidationResult.error(message);
        }

        return ValidationResult.ok();
    }
}