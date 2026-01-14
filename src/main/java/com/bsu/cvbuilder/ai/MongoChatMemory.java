package com.bsu.cvbuilder.ai;

import com.bsu.cvbuilder.domain.entity.chat.AiChat;
import com.bsu.cvbuilder.domain.entity.chat.ChatMessage;
import com.bsu.cvbuilder.domain.entity.chat.MessageRole;
import com.bsu.cvbuilder.service.ChatService;
import lombok.Builder;
import lombok.NonNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.UUID;

@Builder
public record MongoChatMemory(ChatService chatService, int maxMessages) implements ChatMemory {

    @Override
    public void add(@NonNull String conversationId, List<Message> messages) {
        AiChat aiChat = chatService.getChatById(UUID.fromString(conversationId));

        for (Message message : messages) {
            boolean isAlreadyExists = aiChat.getMessages().stream()
                    .anyMatch(existing ->
                            existing.getContent().equals(message.getText())
                                    && existing.getRole().equals(getAirole(message))
                    );
            if (!isAlreadyExists) {
                ChatMessage chatMessage = ChatMessage.builder()
                        .content(message.getText())
                        .role(getAirole(message))
                        .build();
                aiChat.getMessages().add(chatMessage);
            }
        }
        chatService.saveAiChat(aiChat);
    }

    @Override
    @NonNull
    public List<Message> get(@NonNull String conversationId) {
        AiChat aiChat = chatService.getChatById(UUID.fromString(conversationId));
        return aiChat.getMessages().stream()
                .skip(Math.max(0, aiChat.getMessages().size() - maxMessages))
                .map(this::getMessage)
                .limit(maxMessages)
                .toList();
    }

    @Override
    @SuppressWarnings("all")
    public void clear(@NonNull String conversationId) {
        // ignored
    }

    private Message getMessage(ChatMessage aiChatMessage) {
        switch (aiChatMessage.getRole()) {
            case USER -> {
                return new UserMessage(aiChatMessage.getContent());
            }
            case ASSISTANT -> {
                return new AssistantMessage(aiChatMessage.getContent());
            }
            default -> {
                return null;
            }
        }
    }

    private MessageRole getAirole(Message message) {
        switch (message.getMessageType()) {
            case USER -> {
                return MessageRole.USER;
            }
            case ASSISTANT -> {
                return MessageRole.ASSISTANT;
            }
            default -> {
                return null;
            }
        }
    }
}
