package com.bsu.cvbuilder.entity.resume.block.expirience;

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
public class WorkExperience extends AbstractBlock {
    private String id;
    private String company;
    private String position;
    private EmploymentType employmentType;
    private String location;
    private String website;
    
    private Period period;
    private String description;
    private List<String> achievements;
    private List<String> technologies;
    
    @Builder.Default
    private Boolean isCurrent = false;

    @Override
    public String getBlockName() {
        return "workExperience";
    }

    @Data
    @Builder
    public static class Period {
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
    }
}