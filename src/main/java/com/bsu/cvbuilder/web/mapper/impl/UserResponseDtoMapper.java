package com.bsu.cvbuilder.web.mapper.impl;

import com.bsu.cvbuilder.entity.user.UserProfile;
import com.bsu.cvbuilder.web.dto.user.UserResponseDto;
import com.bsu.cvbuilder.web.mapper.Mappable;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserResponseDtoMapper extends Mappable<UserProfile, UserResponseDto> {
}
