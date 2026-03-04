package com.bsu.cvbuilder.domain.dto.ai;

import java.util.List;

public record ResumeFlowFormDto (
        String linkedin,
        List<WorkExperience> workExperiences,
        String currentJobName,
        List<Education> education,
        List<String> skills,
        String highlights,
        String careerGoals
) {}

record WorkExperience(
        String title,
        String company,
        String location,
        String startDate,
        String endDate,
        List<String> responsibilities
) {}

record Education(
        String degree,
        String major,
        String university,
        String location,
        String graduationYear
) {}