package com.bsu.cvbuilder.service.flow.handler;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.flow.AbstractChatStepHandler;
import com.bsu.cvbuilder.service.flow.StepValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class CompletedHandler extends AbstractChatStepHandler {

    public CompletedHandler(PromptRegistryService promptRegistryService, StepValidator stepValidator) {
        super(promptRegistryService, stepValidator);
    }

    @Override
    public ChatFlowStep getStep() {
        return ChatFlowStep.COMPLETED;
    }

    @Override
    public StepAnalysisResult analyzeCompletion(ChatClient chatClient, String chatHistory) {
        String requirements = """
            - It is necessary to check all conditions: %s
            """;

        return stepValidator.validate(chatHistory, requirements);
    }
}
