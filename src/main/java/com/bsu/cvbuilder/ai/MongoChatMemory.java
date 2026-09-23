package com.bsu.cvbuilder.ai;

import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.ChatMessage;
import com.bsu.cvbuilder.domain.entity.MessageRole;
import com.bsu.cvbuilder.service.ChatService;
import lombok.Builder;
import lombok.NonNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder
public record MongoChatMemory(ChatService chatService, int maxMessages) implements ChatMemory {

    @Override
    public void add(@NonNull String conversationId, List<Message> messages) {
        if (conversationId.equals("ignore")) {
            return;
        }

        AiChat aiChat = chatService.getChatById(UUID.fromString(conversationId));

        List<ChatMessage> chatMessages = aiChat.getMessages();
        for (Message message : messages) {
            MessageRole role = getAirole(message);
            ChatMessage last = chatMessages.isEmpty() ? null : chatMessages.getLast();
            boolean isDuplicateOfLast = last != null
                    && Objects.equals(last.getContent(), message.getText())
                    && Objects.equals(last.getRole(), role);
            if (!isDuplicateOfLast) {
                chatMessages.add(ChatMessage.builder()
                        .content(message.getText())
                        .role(role)
                        .build());
            }
        }
        chatService.saveAiChat(aiChat);
    }

    @Override
    @NonNull
    public List<Message> get(@NonNull String conversationId) {
        if (conversationId.equals("ignore")) {
            return List.of();
        }
        AiChat aiChat = chatService.getChatById(UUID.fromString(conversationId));
        return aiChat.getMessages().stream()
                .skip(Math.max(0, aiChat.getMessages().size() - maxMessages))
                .map(this::getMessage)
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
