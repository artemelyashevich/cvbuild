package com.bsu.cvbuilder.domain.dto.auth;

import com.bsu.cvbuilder.domain.dto.notification.NotificationEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto implements Serializable {

    @Builder.Default
    private UUID id =  UUID.randomUUID();

    private NotificationEngine engine;

    private String receiver;

    private String templateName;

    @Builder.Default
    private Integer retryCount = 0;

    @ToString.Exclude
    private Map<String, Object> parameters;
}
