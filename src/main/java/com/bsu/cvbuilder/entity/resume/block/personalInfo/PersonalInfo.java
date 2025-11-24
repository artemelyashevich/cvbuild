package com.bsu.cvbuilder.entity.resume.block.personalInfo;

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
public class PersonalInfo extends AbstractBlock {
    private String firstName;
    private String lastName;
    private String position;
    private String email;
    private String phone;
    private String telegram;
    private String github;
    private String linkedin;
    private String portfolio;
    private String location;
    private String photoUrl;
    private String about;
    
    @Builder.Default
    private Boolean isPhonePublic = true;
    
    @Builder.Default
    private Boolean isEmailPublic = true;

    @Override
    public String getBlockName() {
        return "personalInfo";
    }
}