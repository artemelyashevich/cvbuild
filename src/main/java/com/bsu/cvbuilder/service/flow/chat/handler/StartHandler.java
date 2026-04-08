package com.bsu.cvbuilder.service.flow.chat.handler;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.flow.chat.AbstractChatStepHandler;
import com.bsu.cvbuilder.service.flow.chat.StepValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import static com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep.START;

@Component
public class StartHandler extends AbstractChatStepHandler {

    public StartHandler(PromptRegistryService promptRegistryService, StepValidator stepValidator) {
        super(promptRegistryService, stepValidator);
    }

    @Override
    public ChatFlowStep getStep() {
        return START;
    }

    @Override
    public StepAnalysisResult analyzeCompletion(ChatClient chatClient, String chatHistory) {
        return new StepAnalysisResult(true, null, null);
    }

    @Override
    public ChatFlowStep getNextStep() {
        return ChatFlowStep.PERSONAL_INFO;
    }
}
