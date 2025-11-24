package com.bsu.cvbuilder.entity.resume;

import com.bsu.cvbuilder.entity.resume.block.certificate.Certificate;
import com.bsu.cvbuilder.entity.resume.block.education.Education;
import com.bsu.cvbuilder.entity.resume.block.expirience.WorkExperience;
import com.bsu.cvbuilder.entity.resume.block.language.Language;
import com.bsu.cvbuilder.entity.resume.block.personalInfo.PersonalInfo;
import com.bsu.cvbuilder.entity.resume.block.project.Project;
import com.bsu.cvbuilder.entity.resume.block.skill.Skill;
import com.bsu.cvbuilder.entity.resume.meta.AIRecommendation;
import com.bsu.cvbuilder.entity.resume.meta.ResumeMetadata;
import com.bsu.cvbuilder.entity.resume.meta.ResumeSettings;
import com.bsu.cvbuilder.entity.resume.meta.ResumeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@CompoundIndex(name = "user_status_idx", def = "{'userId': 1, 'status': 1}")
@CompoundIndex(name = "template_user_idx", def = "{'templateId': 1, 'userId': 1}")
public class Resume {
    
    @Id
    @ToString.Include
    private String id;
    
    @Indexed
    @ToString.Include
    private String userId;

    @ToString.Include
    private String title;

    @ToString.Include
    private String slug;
    
    @Indexed
    @ToString.Include
    private String templateId;
    
    @Builder.Default
    @ToString.Include
    private ResumeStatus status = ResumeStatus.DRAFT;
    
    private PersonalInfo personalInfo;

    private String summary;

    private List<WorkExperience> workExperience;

    private List<Education> education;

    private Skill skills;

    private List<Project> projects;

    private List<Language> languages;

    private List<Certificate> certificates;
    
    private ResumeSettings settings;

    private ResumeMetadata metadata;
    
    private List<AIRecommendation> aiRecommendations;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @CreatedDate
    @ToString.Include
    private LocalDateTime createdAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @LastModifiedDate
    @ToString.Include
    private LocalDateTime updatedAt;

    @ToString.Include
    private LocalDateTime publishedAt;
}