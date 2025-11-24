package com.bsu.cvbuilder.entity.resume.block.language;

import com.bsu.cvbuilder.entity.resume.block.AbstractBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Language extends AbstractBlock {
    private String id;
    private String name;
    private LanguageLevel level;
    
    @Builder.Default
    private Boolean hasCertificate = false;
    
    private String certificateName;

    @Override
    public String getBlockName() {
        return "language";
    }
}