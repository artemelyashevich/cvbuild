package com.bsu.cvbuilder.entity.resume.block.skill;

import com.bsu.cvbuilder.entity.resume.block.AbstractBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill extends AbstractBlock {

    @Builder.Default
    private List<TechnicalSkill> programmingLanguages = List.of();
    
    @Builder.Default
    private List<TechnicalSkill> frameworks = List.of();
    
    @Builder.Default
    private List<TechnicalSkill> databases = List.of();
    
    @Builder.Default
    private List<TechnicalSkill> tools = List.of();
    
    @Builder.Default
    private List<TechnicalSkill> platforms = List.of();
    
    @Builder.Default
    private List<String> methodologies = List.of();
    
    @Builder.Default
    private List<String> softSkills = List.of();

    @Override
    public String getBlockName() {
        return "skill";
    }

    @Data
    @Builder
    public static class TechnicalSkill {
        private String name;
        private SkillLevel level;
        private Integer yearsOfExperience;
        private String category;
        private Boolean isHighlighted;
        
        @Builder.Default
        private Integer orderIndex = 0;
    }
}