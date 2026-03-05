package com.bsu.cvbuilder.service.flow.form.validator;

public class LengthValidator implements FieldValidator {

    private final int min;
    private final int max;
    private final String message;

    public LengthValidator(int min, int max, String message) {
        this.min = min;
        this.max = max;
        this.message = message;
    }

    @Override
    public ValidationResult validate(String value) {

        if (value == null) {
            return ValidationResult.ok();
        }

        int length = value.length();

        if (length < min || length > max) {
            return ValidationResult.error(message);
        }

        return ValidationResult.ok();
    }
}