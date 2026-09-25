package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.domain.event.AbstractEvent;
import com.bsu.cvbuilder.domain.event.LoginEvent;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.JwtService;
import com.bsu.cvbuilder.service.NotificationService;
import com.bsu.cvbuilder.service.OtpService;
import com.bsu.cvbuilder.service.SecureDataService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.service.impl.SecurityServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private JwtService jwtService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SecureDataService secureDataService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private OtpService otpService;

    @InjectMocks
    private SecurityServiceImpl securityService;

    @Test
    @DisplayName("authenticate: token preparation failure is AppException 500 (not NPE) and the login event records the error")
    void authenticate_PrepareDataFails_Throws500AndPublishesEvent() {
        var principal = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("login", "alice"), "login");
        var authentication = new UsernamePasswordAuthenticationToken(principal, null);
        var user = UserProfile.builder().id("user-1").login("alice").build();
        when(userProfileService.login("alice")).thenReturn(user);
        when(secureDataService.prepareData(user)).thenThrow(new IllegalStateException("redis down"));

        var ex = assertThrows(AppException.class, () -> securityService.authenticate(authentication));

        assertEquals(500, ex.getStatusCode());
        var captor = ArgumentCaptor.forClass(AbstractEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertInstanceOf(LoginEvent.class, captor.getValue());
        assertEquals(Map.of("error", "redis down"), captor.getValue().getData().get("data"));
    }
}
