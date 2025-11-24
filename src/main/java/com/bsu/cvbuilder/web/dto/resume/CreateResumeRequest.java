package com.bsu.cvbuilder.web.dto.resume;

import com.bsu.cvbuilder.entity.resume.block.personalInfo.PersonalInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateResumeRequest {
    private String title;
    private String templateId;
    private PersonalInfo personalInfo;
}