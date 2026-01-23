package com.bsu.cvbuilder.domain.dto.auth;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {

    private NotificationEngine engine;

    private String receiver;

    private String templateName;

    @ToString.Exclude
    private Map<String, Object> parameters;
}
