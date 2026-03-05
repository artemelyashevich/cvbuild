package com.bsu.cvbuilder.service.flow.form.validator;

import com.bsu.cvbuilder.service.flow.form.domain.ResumeField;

import java.util.List;
import java.util.Map;

public class ValidatorRegistry {

    private static final Map<ResumeField, List<FieldValidator>> VALIDATORS = Map.of(

        ResumeField.FIRST_NAME,
        List.of(
            new RequiredValidator("Введите имя"),
            new LengthValidator(2, 50, "Имя должно быть 2–50 символов")
        ),

        ResumeField.LAST_NAME,
        List.of(
            new RequiredValidator("Введите фамилию"),
            new LengthValidator(2, 50, "Фамилия должна быть 2–50 символов")
        ),

        ResumeField.EMAIL,
        List.of(
            new RequiredValidator("Email обязателен"),
            new EmailValidator()
        ),

        ResumeField.PHONE,
        List.of(
            new PhoneValidator()
        ),

        ResumeField.LINKEDIN,
        List.of(new UrlValidator()),

        ResumeField.GITHUB,
        List.of(new UrlValidator()),

        ResumeField.PORTFOLIO,
        List.of(new UrlValidator()),

        ResumeField.CAREER_GOAL,
        List.of(
            new LengthValidator(30, 400,
                "Опишите карьерную цель в 2–4 предложениях")
        ),

        ResumeField.SKILL,
        List.of(
            new LengthValidator(2, 50,
                "Навык должен быть 2–50 символов")
        ),

        ResumeField.HIGHLIGHT,
        List.of(
            new LengthValidator(10, 200,
                "Достижение должно быть 10–200 символов")
        )
    );

    public static List<FieldValidator> getValidators(ResumeField field) {
        return VALIDATORS.getOrDefault(field, List.of());
    }

}