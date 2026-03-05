package com.bsu.cvbuilder.service.flow.form.validator;

public class PhoneValidator extends RegexValidator {

    public PhoneValidator() {
        super(
            "^[+0-9() -]{7,20}$",
            "Введите корректный номер телефона"
        );
    }

}