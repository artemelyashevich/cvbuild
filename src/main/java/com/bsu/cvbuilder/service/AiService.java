package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.domain.entity.Resume;
import org.springframework.ai.chat.client.ChatClient;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AiService {

    String callFlow(AiRequestDto dto);

    ChatClient.CallResponseSpec callExtractor(String history, UUID chatId);

    String callAnalyzer(String text, UUID chatId);

    ChatClient.CallResponseSpec callAtsOptimization(Resume resume, String jobDescription);

    CompletableFuture<Object> callFlow(String promptName, String content);
}
