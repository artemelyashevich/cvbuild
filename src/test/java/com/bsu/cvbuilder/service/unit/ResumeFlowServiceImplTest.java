package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.*;
import com.bsu.cvbuilder.service.flow.form.ResumeFlowServiceImpl;
import com.bsu.cvbuilder.service.flow.form.domain.ResumePayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeFlowServiceImplTest {

    @Mock
    private AiService aiService;
    @Mock
    private ResumeService resumeService;

    @Test
    @DisplayName("generateResume: fails with 504 instead of blocking forever when the model does not answer")
    void generateResume_ModelHangs_Throws504() {
        var chat = new ApplicationProperties.Chat();
        chat.setFlowTimeout(Duration.ofMillis(100));
        var properties = new ApplicationProperties();
        properties.setChat(chat);
        var service = new ResumeFlowServiceImpl(mock(SecurityService.class), resumeService, aiService,
                mock(JobParserService.class), mock(AnalyzerService.class), mock(ApplicationEventPublisher.class),
                mock(NotificationService.class), properties);

        CompletableFuture<Object> hanging = new CompletableFuture<>();
        when(aiService.callFlow(anyString(), anyString())).thenReturn(hanging);

        var user = UserProfile.builder().id("u1").login("alice").build();
        var payload = new ResumePayload("cv",
                Map.of("firstName", "A", "lastName", "B", "email", "a@b.c", "phone", "1"),
                Map.of("linkedin", "l", "github", "g", "portfolio", "p"),
                List.of(), List.of(), List.of("java"), "highlights", "goals");

        AppException ex = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertThrows(AppException.class, () -> service.generateResume(payload, user)));

        assertEquals(504, ex.getStatusCode());
        assertTrue(hanging.isCancelled());
        verifyNoInteractions(resumeService);
    }
}
