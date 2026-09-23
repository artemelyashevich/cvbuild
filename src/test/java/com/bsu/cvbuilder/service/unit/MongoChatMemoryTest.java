package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.ai.MongoChatMemory;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.ChatMessage;
import com.bsu.cvbuilder.domain.entity.MessageRole;
import com.bsu.cvbuilder.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoChatMemoryTest {

    private static final UUID CHAT_ID = UUID.randomUUID();

    @Mock
    private ChatService chatService;

    private AiChat chatWith(ChatMessage... messages) {
        AiChat chat = AiChat.builder().id(CHAT_ID).messages(new ArrayList<>(List.of(messages))).build();
        when(chatService.getChatById(CHAT_ID)).thenReturn(chat);
        return chat;
    }

    private static ChatMessage message(MessageRole role, String content) {
        return ChatMessage.builder().role(role).content(content).build();
    }

    @Test
    @DisplayName("add: repeated answer earlier in history is still saved")
    void add_RepeatedEarlierMessage_IsSaved() {
        AiChat chat = chatWith(message(MessageRole.USER, "да"), message(MessageRole.ASSISTANT, "Есть ли опыт?"));
        var memory = new MongoChatMemory(chatService, 10);

        memory.add(CHAT_ID.toString(), List.of(new UserMessage("да")));

        assertEquals(3, chat.getMessages().size());
        verify(chatService).saveAiChat(chat);
    }

    @Test
    @DisplayName("add: exact duplicate of the last message is skipped")
    void add_DuplicateOfLastMessage_IsSkipped() {
        AiChat chat = chatWith(message(MessageRole.USER, "да"));
        var memory = new MongoChatMemory(chatService, 10);

        memory.add(CHAT_ID.toString(), List.of(new UserMessage("да")));

        assertEquals(1, chat.getMessages().size());
    }

    @Test
    @DisplayName("get: returns last maxMessages messages with mapped types")
    void get_ManyMessages_ReturnsLastWindow() {
        chatWith(
                message(MessageRole.USER, "1"),
                message(MessageRole.ASSISTANT, "2"),
                message(MessageRole.USER, "3")
        );
        var memory = new MongoChatMemory(chatService, 2);

        List<Message> result = memory.get(CHAT_ID.toString());

        assertEquals(List.of("2", "3"), result.stream().map(Message::getText).toList());
        assertInstanceOf(AssistantMessage.class, result.getFirst());
    }
}
