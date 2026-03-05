package com.bsu.cvbuilder.service.flow.chat.handler;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.flow.chat.AbstractChatStepHandler;
import com.bsu.cvbuilder.service.flow.chat.StepValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ExperienceHandler extends AbstractChatStepHandler {

    public ExperienceHandler(PromptRegistryService promptRegistryService, StepValidator stepValidator) {
        super(promptRegistryService, stepValidator);
    }

    @Override
    public ChatFlowStep getStep() {
        return ChatFlowStep.EXPERIENCE;
    }

    @Override
    public StepAnalysisResult analyzeCompletion(ChatClient chatClient, String chatHistory) {
        String requirements = """
            COMPLETION CRITERIA FOR WORK EXPERIENCE BLOCK:
            This stage is considered complete ONLY IF:
            1. The user explicitly states that they have NO work experience ("never worked", "student").
            2. OR the user describes one or more jobs AND explicitly confirms there is NOTHING MORE TO ADD ("that’s all", "next", "no", "move on").
            
            JOB DESCRIPTION (Quality Criteria):
            - If the user describes a job, they must include: Company, Position, Period, Responsibilities.
            - If anything is missing for the CURRENT job being discussed -> completed = false (missingInfo: "responsibilities/dates").
            
            EXAMPLES:
            - User: "Worked at Google" -> completed = false (position is missing).
            - User: "As an Engineer, 2020-2022, wrote code" -> completed = false (Assistant should ask "Is there any other experience?").
            - User: "No more experience" -> completed = true.
            """;

        return stepValidator.validate(chatHistory, requirements);
    }
}
