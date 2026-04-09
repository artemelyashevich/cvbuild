package com.bsu.cvbuilder.service.flow.chat.handler;

import com.bsu.cvbuilder.domain.dto.ai.ChatFlowStep;
import com.bsu.cvbuilder.domain.dto.ai.StepAnalysisResult;
import com.bsu.cvbuilder.service.PromptRegistryService;
import com.bsu.cvbuilder.service.flow.chat.AbstractChatStepHandler;
import com.bsu.cvbuilder.service.flow.chat.StepValidator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PersonalInfoHandler extends AbstractChatStepHandler {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{7,}");

    public PersonalInfoHandler(PromptRegistryService promptRegistryService, StepValidator stepValidator) {
        super(promptRegistryService, stepValidator);
    }

    @Override
    public ChatFlowStep getStep() {
        return ChatFlowStep.PERSONAL_INFO;
    }

    @Override
    public StepAnalysisResult analyzeCompletion(ChatClient chatClient, String chatHistory) {
        boolean hasEmail = EMAIL_PATTERN.matcher(chatHistory).find();
        boolean hasPhone = PHONE_PATTERN.matcher(chatHistory).find();

        String requirements = """
            CHECK REQUIRED FIELDS:
            1. Full Name (First Name): Check presence in the text. (If present -> OK, if missing -> REQUIRED).
            2. Email: %s
            3. Phone: %s
            4. Role: Check presence in the text. (If present -> OK, if missing -> REQUIRED).
            
            IMPORTANT: If Email or Phone is marked as "REQUIRED", you MUST return completed = false.
            """.formatted(
                hasEmail ? "OK (Found)" : "REQUIRED",
                hasPhone ? "OK (Found)" : "REQUIRED"
        );

        return stepValidator.validate(chatHistory, requirements);
    }

    @Override
    public ChatFlowStep getNextStep() {
        return ChatFlowStep.EDUCATION;
    }
}
