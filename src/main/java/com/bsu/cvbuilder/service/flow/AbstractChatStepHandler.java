package com.bsu.cvbuilder.service.flow;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;

@RequiredArgsConstructor
public abstract class AbstractChatStepHandler {

    private final PromptRegistryService promptRegistryService;
    protected final StepValidator stepValidator;

    public abstract ChatFlowStep getStep();

    public abstract StepAnalysisResult analyzeCompletion(ChatClient chatClient, String chatHistory);

    public ChatFlowStep getNextStep() {
        return ChatFlowStep.COMPLETED;
    }

    public String getSystemPrompt() {
        return promptRegistryService.getPrompt(getStep().name().toLowerCase());
    }

    public boolean isStepCompleted(String userLastMessage) {
        return false;
    }
}