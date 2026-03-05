package com.bsu.cvbuilder.service.flow.chat.handler;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.flow.chat.AbstractChatStepHandler;
import com.bsu.cvbuilder.service.flow.chat.StepValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class SkillsHandler extends AbstractChatStepHandler {

    public SkillsHandler(PromptRegistryService promptRegistryService, StepValidator stepValidator) {
        super(promptRegistryService, stepValidator);
    }

    @Override
    public ChatFlowStep getStep() {
        return ChatFlowStep.SKILLS;
    }

    @Override
    public StepAnalysisResult analyzeCompletion(ChatClient chatClient, String chatHistory) {
        String requirements = """
                    CRITERIA FOR COMPLETION:
                   The user has listed key skills (Hard Skills).
                   OR the user has explicitly indicated that the list is finished (“all done,” “that’s it,” “let’s move on,” “I don’t know any more”).
                   IMPORTANT:
                   If the list of skills looks complete (3+ skills) and the user does not add anything new → completed = true, but it’s better to confirm by asking, “Is there anything else?”
                   If the user wrote only one skill (e.g., “Java”), ask for more → completed = false.
                """;
        return stepValidator.validate(chatHistory, requirements);
    }

    @Override
    public ChatFlowStep getNextStep() {
        return ChatFlowStep.EXPERIENCE;
    }
}
