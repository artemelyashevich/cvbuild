package com.bsu.cvbuilder.service.flow.form.validator;

public class EmailValidator extends RegexValidator {

    public EmailValidator() {
        super(
            "^[A-Za-z0-9+_.-]+@(.+)$",
            "Введите корректный email"
        );
    }

}