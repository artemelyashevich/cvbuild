package com.bsu.cvbuilder.entity.resume.block.project;

import com.bsu.cvbuilder.entity.resume.block.AbstractBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project extends AbstractBlock {
    private String id;
    private String name;
    private String description;
    private String role;
    private String client;
    
    private Period period;
    private List<String> technologies;
    private List<String> responsibilities;
    private List<String> achievements;
    
    private String githubUrl;
    private String demoUrl;
    private String liveUrl;
    
    @Builder.Default
    private Boolean isCommercial = false;
    
    @Builder.Default
    private Boolean isOpenSource = false;

    @Override
    public String getBlockName() {
        return "project";
    }

    @Data
    @Builder
    public static class Period {
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
    }
}