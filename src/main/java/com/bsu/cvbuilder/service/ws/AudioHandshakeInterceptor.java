package com.bsu.cvbuilder.service.ws;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

public class AudioHandshakeInterceptor implements HandshakeInterceptor {

    public static final String CHAT_ID = "chatId";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request.getPrincipal() == null) {
            return false;
        }

        String chatIdRaw = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst(CHAT_ID);
        if (chatIdRaw == null || chatIdRaw.isBlank()) {
            return false;
        }

        try {
            attributes.put(CHAT_ID, UUID.fromString(chatIdRaw));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }
}
