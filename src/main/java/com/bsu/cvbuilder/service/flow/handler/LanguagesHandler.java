package com.bsu.cvbuilder.service.flow.handler;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.flow.AbstractChatStepHandler;
import com.bsu.cvbuilder.service.flow.StepValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class LanguagesHandler extends AbstractChatStepHandler {

    public LanguagesHandler(PromptRegistryService promptRegistryService, StepValidator stepValidator) {
        super(promptRegistryService, stepValidator);
    }

    @Override
    public ChatFlowStep getStep() {
        return ChatFlowStep.LANGUAGES;
    }

    @Override
    public StepAnalysisResult analyzeCompletion(ChatClient chatClient, String chatHistory) {
        String requirements = """
            COMPLETION CRITERIA:
            1. The user listed at least one language (e.g., Russian, English).
            2. For EACH listed language, the proficiency level must be clear (A1-C2, basic, native, with dictionary, etc.).
            
            IMPORTANT:
            - If the user wrote just "English," ask for the level -> completed = false.
            - If the user wrote "Only Russian" or "I don’t know any other languages" -> completed = true.
            """;

        return stepValidator.validate(chatHistory, requirements);
    }

    @Override
    public ChatFlowStep getNextStep() {
        return ChatFlowStep.SKILLS;
    }
}
