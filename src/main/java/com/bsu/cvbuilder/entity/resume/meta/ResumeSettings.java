package com.bsu.cvbuilder.entity.resume.meta;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeSettings {
    @Builder.Default
    private String theme = "modern";
    
    @Builder.Default
    private String colorScheme = "blue";
    
    @Builder.Default
    private String fontFamily = "inter";
    
    @Builder.Default
    private Integer fontSize = 14;
    
    @Builder.Default
    private Boolean showPhoto = true;
    
    @Builder.Default
    private String language = "ru";
    
    @Builder.Default
    private Boolean compactView = false;
    
    @Builder.Default
    private Integer skillsDisplayLimit = 15;
    
    @Builder.Default
    private Boolean showSummary = true;
    
    @Builder.Default
    private Boolean showProjects = true;
    
    @Builder.Default
    private Boolean showLanguages = true;
    
    @Builder.Default
    private Boolean showCertificates = true;
    
    @Builder.Default
    private Boolean showGithubStats = false;
}