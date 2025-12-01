package com.bsu.cvbuilder.entity.chat;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ChatState {
    WELCOME("welcome"),
    Q1_VACANCY(""),
    Q2_SKILLS(""),
    Q3_PERSONAL_INFO(""),
    Q4_PHOTO(""),
    Q5_EXPERIENCE(""),
    Q6_ABOUT(""),
    Q7_LOCATION(""),
    Q8_DESCRIPTION(""),
    Q9_ATS_CHECK(""),
    GENERATING(""),
    SHOW_RESULT(""),
    FOLLOW_UP(""),
    EDITOR("");

    private static final String PATH_TO_PROMPT = "/chat/%s";

    private final String path;

    public String getPath() {
        return PATH_TO_PROMPT.formatted(path);
    }
}