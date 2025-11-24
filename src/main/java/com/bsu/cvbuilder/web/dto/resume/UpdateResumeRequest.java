package com.bsu.cvbuilder.web.dto.resume;

import com.bsu.cvbuilder.entity.resume.block.education.Education;
import com.bsu.cvbuilder.entity.resume.block.expirience.WorkExperience;
import com.bsu.cvbuilder.entity.resume.block.language.Language;
import com.bsu.cvbuilder.entity.resume.block.personalInfo.PersonalInfo;
import com.bsu.cvbuilder.entity.resume.block.project.Project;
import com.bsu.cvbuilder.entity.resume.block.skill.Skill;
import com.bsu.cvbuilder.entity.resume.meta.ResumeSettings;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateResumeRequest {
    private String title;
    private PersonalInfo personalInfo;
    private String summary;
    private List<WorkExperience> workExperience;
    private List<Education> education;
    private Skill skills;
    private List<Project> projects;
    private List<Language> languages;
    private ResumeSettings settings;
}