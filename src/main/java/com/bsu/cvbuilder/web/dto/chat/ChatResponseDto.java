package com.bsu.cvbuilder.web.dto.chat;

import com.bsu.cvbuilder.entity.chat.ChatMessage;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {
    private List<ChatMessage> chatMessages;
}
