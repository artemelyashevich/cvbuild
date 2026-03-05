package com.bsu.cvbuilder.service.flow.form.domain;


import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Getter
public enum ResumeFlowStep {
    PERSONAL_INFORMATION(
            1,
            "Введите личную информацию",
            List.of(
                    ResumeField.FIRST_NAME,
                    ResumeField.LAST_NAME,
                    ResumeField.EMAIL,
                    ResumeField.PHONE
            ),
            List.of(
                    new FollowUpQuestion(ResumeField.FIRST_NAME, "Как вас зовут?"),
                    new FollowUpQuestion(ResumeField.LAST_NAME, "Ваша фамилия?"),
                    new FollowUpQuestion(ResumeField.EMAIL, "Введите ваш email для связи."),
                    new FollowUpQuestion(ResumeField.PHONE, "Введите номер телефона (по желанию).")
            ),
            false
    ),

    LINKS(
            2,
            "Добавьте профессиональные ссылки",
            List.of(
                    ResumeField.LINKEDIN,
                    ResumeField.GITHUB,
                    ResumeField.PORTFOLIO
            ),
            List.of(
                    new FollowUpQuestion(ResumeField.LINKEDIN, "Есть ли у вас LinkedIn профиль?"),
                    new FollowUpQuestion(ResumeField.GITHUB, "Укажите ссылку на GitHub (если есть)."),
                    new FollowUpQuestion(ResumeField.PORTFOLIO, "Есть ли у вас портфолио или личный сайт?")
            ),
            false
    ),

    JOB(
            3,
            "Опишите опыт работы",
            List.of(
                    ResumeField.COMPANY,
                    ResumeField.POSITION,
                    ResumeField.JOB_PERIOD,
                    ResumeField.RESPONSIBILITIES,
                    ResumeField.ACHIEVEMENTS
            ),
            List.of(
                    new FollowUpQuestion(ResumeField.COMPANY, "В какой компании вы работали?"),
                    new FollowUpQuestion(ResumeField.POSITION, "Какая у вас была должность?"),
                    new FollowUpQuestion(ResumeField.JOB_PERIOD, "В какой период вы работали?"),
                    new FollowUpQuestion(ResumeField.RESPONSIBILITIES, "Какие были основные обязанности?"),
                    new FollowUpQuestion(ResumeField.ACHIEVEMENTS, "Какие ключевые достижения?")
            ),
            false
    ),

    EDUCATION(
            4,
            "Добавьте образование",
            List.of(
                    ResumeField.UNIVERSITY,
                    ResumeField.DEGREE,
                    ResumeField.FIELD_OF_STUDY,
                    ResumeField.EDUCATION_PERIOD
            ),
            List.of(
                    new FollowUpQuestion(ResumeField.UNIVERSITY, "В каком университете вы учились?"),
                    new FollowUpQuestion(ResumeField.DEGREE, "Какая степень? (Bachelor / Master)"),
                    new FollowUpQuestion(ResumeField.FIELD_OF_STUDY, "Какая специальность?"),
                    new FollowUpQuestion(ResumeField.EDUCATION_PERIOD, "Годы обучения?")
            ),
            false
    ),

    SKILLS(
            5,
            "Добавьте навыки",
            List.of(ResumeField.SKILL),
            List.of(
                    new FollowUpQuestion(ResumeField.SKILL,
                            "Перечислите ваши ключевые навыки (например: Java, Spring, Docker).")
            ),
            false
    ),

    HIGHLIGHTS(
            6,
            "Добавьте ключевые достижения",
            List.of(ResumeField.HIGHLIGHT),
            List.of(
                    new FollowUpQuestion(ResumeField.HIGHLIGHT,
                            "Назовите 3–5 ваших главных профессиональных достижений.")
            ),
            true
    ),

    CAREER_GOALS(
            7,
            "Опишите карьерные цели",
            List.of(ResumeField.CAREER_GOAL),
            List.of(
                    new FollowUpQuestion(ResumeField.CAREER_GOAL,
                            "Какую работу вы ищете и в каком направлении хотите развиваться?")
            ),
            true
    );

    private final Integer priority;
    private final String description;
    private final List<ResumeField> fields;
    private final List<FollowUpQuestion> questions;
    private final Boolean voiceInput;

    ResumeFlowStep(
            Integer priority, String description,
            List<ResumeField> fields,
            List<FollowUpQuestion> questions, Boolean voiceInput
    ) {
        this.priority = priority;
        this.description = description;
        this.fields = fields;
        this.questions = questions;
        this.voiceInput = voiceInput;
    }

    public static List<ResumeFlowStep> getResumeFlowRoadmap() {
        return Stream.of(values())
                .sorted(Comparator.comparing(ResumeFlowStep::getPriority))
                .toList();
    }

    public Map<String, Object> getData() {
        return Map.of(
                "name", name().toLowerCase(),
                "voiceInput", voiceInput,
                "questions", questions
        );
    }
}