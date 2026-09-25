package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.ResumeNotificationEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.AnalyzerService;
import com.bsu.cvbuilder.service.JobParserService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.impl.AtsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtsServiceImplTest {

    private static final UserProfile USER = UserProfile.builder().id("user-1").login("alice").email("alice@mail.test").build();
    private static final String LINK = "https://jobs.test/1";

    @Mock
    private ResumeService resumeService;
    @Mock
    private JobParserService jobParserService;
    @Mock
    private AiService aiService;
    @Mock
    private AnalyzerService analyzerService;
    @Mock
    private SecurityService securityService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private AtsServiceImpl atsService;

    @BeforeEach
    void setUp() {
        // Runs "async" work in the caller thread so the outcome is observable.
        atsService = new AtsServiceImpl(resumeService, jobParserService, aiService, analyzerService,
                securityService, applicationEventPublisher, Runnable::run);
    }

    @Test
    @DisplayName("optimize: parses the vacancy, expands it with AI and stores the ATS copy")
    void optimize_RunsWholePipeline() {
        var resume = Resume.builder().id("res-1").build();
        var expansion = mock(ChatClient.CallResponseSpec.class);
        when(jobParserService.parse(LINK)).thenReturn("raw job");
        when(aiService.callExpansion("raw job")).thenReturn(expansion);
        when(expansion.content()).thenReturn("expanded job");

        assertSame(resume, atsService.optimize(resume, LINK));

        verify(analyzerService).ats(resume, "expanded job");
    }

    @Test
    @DisplayName("optimizeAsync: failure in background reports ATS_FAILED after ATS_ACCEPTED")
    void optimizeAsync_AnalyzerFails_PublishesFailed() {
        var resume = Resume.builder().id("res-1").build();
        var expansion = mock(ChatClient.CallResponseSpec.class);
        when(securityService.findCurrentUser()).thenReturn(USER);
        when(resumeService.findById("res-1")).thenReturn(resume);
        when(jobParserService.parse(LINK)).thenReturn("raw job");
        when(aiService.callExpansion("raw job")).thenReturn(expansion);
        when(expansion.content()).thenReturn("expanded job");
        doThrow(new IllegalStateException("model down")).when(analyzerService).ats(resume, "expanded job");

        atsService.optimizeAsync("res-1", LINK);

        InOrder order = inOrder(applicationEventPublisher);
        order.verify(applicationEventPublisher).publishEvent(event(ResumeNotificationEvent.Kind.ATS_ACCEPTED));
        order.verify(applicationEventPublisher).publishEvent(event(ResumeNotificationEvent.Kind.ATS_FAILED));
    }

    @Test
    @DisplayName("optimizeAsync: foreign resume fails in the caller thread, nothing is accepted")
    void optimizeAsync_AccessDenied_NothingPublished() {
        when(securityService.findCurrentUser()).thenReturn(USER);
        when(resumeService.findById("res-1")).thenThrow(new AppException("denied", 403));

        assertThrows(AppException.class, () -> atsService.optimizeAsync("res-1", LINK));

        verifyNoInteractions(applicationEventPublisher, aiService, analyzerService);
    }

    private static ResumeNotificationEvent event(ResumeNotificationEvent.Kind kind) {
        return new ResumeNotificationEvent("alice", "alice@mail.test", "res-1", kind);
    }
}
