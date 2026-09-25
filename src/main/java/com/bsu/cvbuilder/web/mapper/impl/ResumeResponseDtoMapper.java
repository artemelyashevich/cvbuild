package com.bsu.cvbuilder.web.mapper.impl;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.web.dto.resume.ResumeResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ResumeResponseDtoMapper {

    ResumeResponseDto toDto(Resume resume);

    /**
     * List view without resume content: only identity, dates and settings.
     */
    default ResumeResponseDto toMaskedDto(Resume resume) {
        return toDto(Resume.builder()
                .id(resume.getId())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .resumeSettings(resume.getResumeSettings())
                .build());
    }
}
