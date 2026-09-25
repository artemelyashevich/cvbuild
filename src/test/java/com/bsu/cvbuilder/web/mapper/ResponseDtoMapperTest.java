package com.bsu.cvbuilder.web.mapper;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.ChatMessage;
import com.bsu.cvbuilder.domain.entity.MessageRole;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.web.mapper.impl.AiChatResponseDtoMapper;
import com.bsu.cvbuilder.web.mapper.impl.ResumeResponseDtoMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Response DTOs must keep the JSON that clients received when entities were returned directly.
 */
class ResponseDtoMapperTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 10, 15, 30);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final ResumeResponseDtoMapper resumeMapper = Mappers.getMapper(ResumeResponseDtoMapper.class);
    private final AiChatResponseDtoMapper chatMapper = Mappers.getMapper(AiChatResponseDtoMapper.class);

    @Test
    @DisplayName("ResumeResponseDto serializes exactly like the Resume entity")
    void resumeDto_SameJsonAsEntity() {
        Resume resume = Resume.builder()
                .id("res-1")
                .blocks(Map.of("Summary", "text"))
                .chatId(UUID.randomUUID().toString())
                .resumeSettings(Resume.ResumeSettings.builder().name("cv").resumeTemplate("basic").ownerId("u1").ownerLogin("alice").build())
                .createdAt(NOW)
                .updatedAt(NOW)
                .atsId("res-2")
                .isAts(true)
                .generatedWithChat(false)
                .version(3L)
                .build();

        assertEquals(objectMapper.valueToTree(resume), objectMapper.valueToTree(resumeMapper.toDto(resume)));
    }

    @Test
    @DisplayName("Masked ResumeResponseDto keeps only identity, dates and settings like the old controller code")
    void maskedResumeDto_SameJsonAsOldMaskedEntity() {
        Resume resume = Resume.builder()
                .id("res-1")
                .blocks(Map.of("Summary", "secret"))
                .resumeSettings(Resume.ResumeSettings.builder().name("cv").build())
                .createdAt(NOW)
                .updatedAt(NOW)
                .atsId("res-2")
                .isAts(true)
                .build();
        Resume oldMasked = Resume.builder()
                .id(resume.getId())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .resumeSettings(resume.getResumeSettings())
                .build();

        assertEquals(objectMapper.valueToTree(oldMasked), objectMapper.valueToTree(resumeMapper.toMaskedDto(resume)));
    }

    @Test
    @DisplayName("AiChatResponseDto serializes exactly like the AiChat entity")
    void chatDto_SameJsonAsEntity() {
        AiChat chat = AiChat.builder()
                .id(UUID.randomUUID())
                .userId("u1")
                .messages(new ArrayList<>(List.of(ChatMessage.builder()
                        .id(UUID.randomUUID()).role(MessageRole.USER).content("hi").timestamp(NOW).build())))
                .templateId("basic")
                .isFinished(true)
                .chatFlowStep(ChatFlowStep.COMPLETED)
                .version(1L)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        assertEquals(objectMapper.valueToTree(chat), objectMapper.valueToTree(chatMapper.toDto(chat)));
    }
}
