package com.bsu.cvbuilder.service.flow.form.validator;

public class UrlValidator extends RegexValidator {

    public UrlValidator() {
        super(
            "^(https?://).+",
            "Введите корректную ссылку (должна начинаться с http или https)"
        );
    }

}