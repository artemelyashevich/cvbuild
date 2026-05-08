package com.bsu.cvbuilder.configuration;

import com.bsu.cvbuilder.service.ws.AudioHandshakeInterceptor;
import com.bsu.cvbuilder.service.ws.AudioWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class VoiceWebSocketConfiguration implements WebSocketConfigurer {

    private final AudioWebSocketHandler audioWebSocketHandler;
    private final AudioHandshakeInterceptor audioHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(audioWebSocketHandler, "/audio")
                .addInterceptors(audioHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
