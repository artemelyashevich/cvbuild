package com.bsu.cvbuilder.web.mapper.impl;

import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.web.dto.chat.AiChatResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AiChatResponseDtoMapper {

    AiChatResponseDto toDto(AiChat chat);
}
