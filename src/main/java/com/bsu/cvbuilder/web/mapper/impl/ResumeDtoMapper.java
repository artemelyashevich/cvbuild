package com.bsu.cvbuilder.web.mapper.impl;

import com.bsu.cvbuilder.entity.resume.Resume;
import com.bsu.cvbuilder.web.dto.resume.CreateResumeRequest;
import com.bsu.cvbuilder.web.dto.resume.ResumeResponse;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import com.bsu.cvbuilder.web.mapper.Mappable;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ResumeDtoMapper extends Mappable<Resume, CreateResumeRequest> {

    ResumeResponse toResponse(Resume resume);

    List<ResumeResponse> toResponseList(List<Resume> resumes);

    Resume toEntity(UpdateResumeRequest request);
}
