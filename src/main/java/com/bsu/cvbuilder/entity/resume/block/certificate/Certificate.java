package com.bsu.cvbuilder.entity.resume.block.certificate;

import com.bsu.cvbuilder.entity.resume.block.AbstractBlock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate extends AbstractBlock {

    private String id;
    private String name;
    private String issuer;
    private LocalDate issueDate;
    private LocalDate expirationDate;
    private String credentialId;
    private String credentialUrl;
    
    @Builder.Default
    private Boolean doesNotExpire = false;

    @Override
    public String getBlockName() {
        return "certificate";
    }
}