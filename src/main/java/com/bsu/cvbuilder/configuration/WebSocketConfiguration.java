package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.domain.dto.auth.SecurityProvider;
import com.bsu.cvbuilder.domain.dto.auth.TokenType;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;


import static com.bsu.cvbuilder.util.OAuthUtil.getOAuth2AuthenticationToken;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                StompCommand command = accessor.getCommand();

                if (StompCommand.CONNECT.equals(command) ||
                        StompCommand.SUBSCRIBE.equals(command) ||
                        StompCommand.SEND.equals(command)) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        try {
                            String login = jwtService.extractLogin(token, TokenType.ACCESS);
                            UserProfile.Role role = jwtService.extractRole(token, TokenType.ACCESS);
                            var ctx = SecurityContextHolder.getContext();
                            OAuth2AuthenticationToken authentication = getOAuth2AuthenticationToken(login, role);
                            ctx.setAuthentication(authentication);
                            SecurityContextHolder.setContext(ctx);
                            accessor.setUser(authentication);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            return null;
                        }
                    }
                }
                return message;
            }
        });
    }
}