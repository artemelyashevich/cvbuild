package com.bsu.cvbuilder.web.mapper.impl;

import com.bsu.cvbuilder.domain.dto.question.ChatTemplateDto;
import com.bsu.cvbuilder.domain.entity.chat.QuestionTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface QuestionTemplateMapper {

    ChatTemplateDto toDto(QuestionTemplate chatTemplate);
    QuestionTemplate toEntity(ChatTemplateDto questionTemplateDto);
    List<ChatTemplateDto> toDto(List<QuestionTemplate> chatTemplates);
}
