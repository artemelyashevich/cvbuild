package com.bsu.cvbuilder.service.flow.form.domain;

public enum ResumeField {

    FIRST_NAME(true),
    LAST_NAME(true),
    EMAIL(true),
    PHONE(false),

    LINKEDIN(false),
    GITHUB(false),
    PORTFOLIO(false),

    COMPANY(true),
    POSITION(true),
    JOB_PERIOD(true),
    RESPONSIBILITIES(true),
    ACHIEVEMENTS(false),

    UNIVERSITY(false),
    DEGREE(false),
    FIELD_OF_STUDY(false),
    EDUCATION_PERIOD(false),

    SKILL(true),

    HIGHLIGHT(false),

    CAREER_GOAL(false);

    private final boolean required;

    ResumeField(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }
}