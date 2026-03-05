package com.bsu.cvbuilder.service.flow.form.validator;

public class RequiredValidator implements FieldValidator {

    private final String message;

    public RequiredValidator(String message) {
        this.message = message;
    }

    @Override
    public ValidationResult validate(String value) {

        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error(message);
        }

        return ValidationResult.ok();
    }
}