package com.bsu.cvbuilder.service.flow.handler;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.flow.AbstractChatStepHandler;
import com.bsu.cvbuilder.service.flow.StepValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class EducationHandler extends AbstractChatStepHandler {

    public EducationHandler(PromptRegistryService promptRegistryService, StepValidator stepValidator) {
        super(promptRegistryService, stepValidator);
    }

    @Override
    public ChatFlowStep getStep() {
        return ChatFlowStep.EDUCATION;
    }

    @Override
    public StepAnalysisResult analyzeCompletion(ChatClient chatClient, String chatHistory) {
        String requirements = """
            COMPLETION CRITERIA:
            1. The user listed at least one educational institution (university, college, courses) OR explicitly stated that they have no education / nothing more to add.
            2. For each listed institution, the following must be specified: Name, Major/Faculty, Years of study (or graduation date).
            
            IMPORTANT:
            - If the user wrote "No more," "That’s enough," "That’s all," or something similar (indicating they want to move to the next stage) -> completed = true.
            - If the user listed one institution but did not provide the year -> completed = false (missingInfo: "graduation year").
            """;

        return stepValidator.validate(chatHistory, requirements);
    }

    @Override
    public ChatFlowStep getNextStep() {
        return ChatFlowStep.LANGUAGES;
    }
}
