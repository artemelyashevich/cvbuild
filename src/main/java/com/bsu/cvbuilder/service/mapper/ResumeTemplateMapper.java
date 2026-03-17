package com.bsu.cvbuilder.service.mapper;

import com.bsu.cvbuilder.domain.dto.template.CreateTemplateRequest;
import com.bsu.cvbuilder.domain.entity.ResumeTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ResumeTemplateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ResumeTemplate toEntity(CreateTemplateRequest request);

    ResumeTemplate.Layout map(CreateTemplateRequest.Layout layout);

    ResumeTemplate.Styles map(CreateTemplateRequest.Styles styles);
}
