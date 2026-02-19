package com.bsu.cvbuilder.service.mapper;

import com.bsu.cvbuilder.domain.dto.auth.RegisterAuthDto;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    UserProfile toUserProfile(RegisterAuthDto authRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "login", ignore = true)
    void updateEntity(UserProfile source, @MappingTarget UserProfile target);
}
