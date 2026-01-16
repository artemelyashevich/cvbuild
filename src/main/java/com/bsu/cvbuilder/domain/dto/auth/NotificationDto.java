package com.bsu.cvbuilder.domain.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {

    private NotificationEngine engine;

    private String receiver;

    private String templateName;

    private Map<String, Object> parameters;
}
