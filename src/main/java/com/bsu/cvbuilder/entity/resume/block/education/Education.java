package com.bsu.cvbuilder.entity.resume.block.education;

import com.bsu.cvbuilder.entity.resume.block.AbstractBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Education extends AbstractBlock {
    private String id;
    private String institution;
    private String degree;
    private String fieldOfStudy;
    private String description;
    
    private Period period;
    private Double gpa;
    private String website;

    @Override
    public String getBlockName() {
        return "education";
    }

    @Data
    @Builder
    public static class Period {
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
    }
}