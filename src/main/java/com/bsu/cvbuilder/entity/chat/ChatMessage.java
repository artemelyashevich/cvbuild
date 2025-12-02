package com.bsu.cvbuilder.entity.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String id;
    private MessageRole role;
    private String content;
    private String questionId;
    private boolean isAnswer;
    private LocalDateTime timestamp;
}