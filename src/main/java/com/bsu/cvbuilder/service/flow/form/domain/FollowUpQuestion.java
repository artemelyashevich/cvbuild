package com.bsu.cvbuilder.service.flow.form.domain;

public class FollowUpQuestion {

    private final ResumeField field;
    private final String question;

    public FollowUpQuestion(ResumeField field, String question) {
        this.field = field;
        this.question = question;
    }

    public ResumeField getField() {
        return field;
    }

    public String getQuestion() {
        return question;
    }
}