package com.bsu.cvbuilder.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private UUID id;
    private MessageRole role;
    private String content;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}