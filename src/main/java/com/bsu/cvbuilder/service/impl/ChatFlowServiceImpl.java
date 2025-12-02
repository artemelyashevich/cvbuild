package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.dto.chat.AnswerRequest;
import com.bsu.cvbuilder.entity.chat.ChatMessage;
import com.bsu.cvbuilder.entity.chat.ChatQuestion;
import com.bsu.cvbuilder.entity.chat.ChatSession;
import com.bsu.cvbuilder.entity.chat.MessageRole;
import com.bsu.cvbuilder.entity.chat.SessionState;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.ChatSessionRepository;
import com.bsu.cvbuilder.service.ChatFlowService;
import com.bsu.cvbuilder.service.MessageSourceService;
import com.bsu.cvbuilder.service.QuestionTemplateService;
import com.bsu.cvbuilder.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFlowServiceImpl implements ChatFlowService {

    private final SecurityService securityService;
    private final QuestionTemplateService questionTemplateService;
    private final ChatSessionRepository chatSessionRepository;
    private final MessageSourceService messageSourceService;

    @Override
    @Transactional
    public ChatSession startSession(String templateId) {
        log.info("--- CHAT FLOW: <<START SESSION>> ---");

        var template = questionTemplateService.findById(templateId);
        var user = securityService.findCurrentUser();

        var session = new ChatSession();
        session.setUserId(user.getId());
        session.setTemplateId(template.getId());
        session.setState(SessionState.IN_PROGRESS);

        var message = new ChatMessage();
        message.setQuestionId(UUID.randomUUID().toString());
        message.setRole(MessageRole.ASSISTANT);
        message.setContent(messageSourceService.findMessage("chat.welcome", user.getEmail()));
        message.setTimestamp(LocalDateTime.now());
        message.setAnswer(false);

        session.setCurrentQuestionId(message.getQuestionId());
        session.setCurrentQuestionOrder(0);
        session.getMessageHistory().add(message);

        log.info("--- CHAT FLOW: <<GENERATE WELCOME MESSAGES>> ---");
        return chatSessionRepository.save(session);
    }

    @Override
    @Transactional
    public ChatSession processAnswer(String sessionId, AnswerRequest answer) {
        log.info("--- CHAT FLOW '{}': <<PROCESS ANSWER>> ---", sessionId);

        var session = chatSessionRepository.findById(sessionId).orElseThrow(() -> {
            var message = "Chat message with id '%s' not found".formatted(sessionId);
            log.warn(message);
            return new AppException(message, 404);
        });

        var template = questionTemplateService.findById(session.getTemplateId());

        var user = securityService.findCurrentUser();

        var currentQuestion = session.getMessageHistory().stream()
                .filter(question -> question.getRole().equals(MessageRole.ASSISTANT) && !question.isAnswer())
                .filter(question -> question.getQuestionId().equalsIgnoreCase(session.getCurrentQuestionId()))
                .findFirst()
                .orElseThrow(() -> {
                    var message = "Chat: current message with id '%s' not found".formatted(session.getCurrentQuestionOrder());
                    log.warn(message);
                    return new AppException(message, 404);
                });

        session.getMessageHistory().add(
                ChatMessage.builder()
                        .questionId(currentQuestion.getQuestionId())
                        .content(answer.getValue())
                        .role(MessageRole.USER)
                        .isAnswer(true)
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        var newQuestion = template.getQuestions().stream()
                .filter(question -> question.getOrder() > session.getCurrentQuestionOrder())
                .filter(question -> checkConditions(question, session))
                .min(Comparator.comparing(ChatQuestion::getOrder));

        if (newQuestion.isEmpty()) {
            session.setState(SessionState.WAITING_APPROVAL);
            session.getMessageHistory().add(
                    ChatMessage.builder()
                            .questionId(UUID.randomUUID().toString())
                            .content(messageSourceService.findMessage("chat.lastQuestion", user.getEmail()))
                            .role(MessageRole.ASSISTANT)
                            .isAnswer(true)
                            .timestamp(LocalDateTime.now())
                            .build()
            );
            chatSessionRepository.save(session);
            // TODO: generate resume

        }

        log.info("--- CHAT FLOW '{}': <<PROCESSED ANSWER SUCCESSFULLY>> ---", sessionId);
        return null;
    }

    private boolean checkConditions(ChatQuestion question, ChatSession session) {
        if (question.getConditions() == null || question.getConditions().isEmpty()) {
            return true;
        }

        return question.getConditions().stream()
                .allMatch(condition -> {
                    var dependedAnswer = session.getMessageHistory().stream()
                            .filter(q -> q.getQuestionId().equals(condition.getQuestionId()))
                            .findFirst()
                            .orElseThrow(() -> {
                                var message = "Depended message with if %s was not found".formatted(condition.getQuestionId());
                                log.warn(message);
                                return new AppException(message, 400);
                            });
                    var actualAnswer = dependedAnswer.getContent();
                    var expectedAnswer = condition.getValue();

                    return switch (condition.getOperator()) {
                        case ChatQuestion.Condition.Operator.EQUALS -> actualAnswer.equals(expectedAnswer);
                        case NOT_EQUALS -> !actualAnswer.equals(expectedAnswer);
                        case CONTAINS -> actualAnswer.contains(expectedAnswer);
                        default -> false;
                    };
                });
    }
}
