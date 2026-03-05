package com.bsu.cvbuilder.service.flow.form.domain;

import java.util.List;
import java.util.Map;

public record ResumePayload(
        Map<String, String> personalInformation,
        Map<String, String> links,
        List<Map<String, String>> job,
        List<Map<String, String>> education,
        List<String> skills,
        String highlights,
        String careerGoals
) {}