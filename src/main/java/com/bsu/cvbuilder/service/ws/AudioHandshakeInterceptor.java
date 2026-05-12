package com.bsu.cvbuilder.service.ws;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AudioHandshakeInterceptor implements HandshakeInterceptor {

    private static final String CHAT_ID = "chatId";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        String query = request.getURI().getQuery();
        if (query == null || query.isBlank()) {
            return false;
        }

        Map<String, String> params = parseQuery(query);

        String chatIdRaw = params.get(CHAT_ID);
        if (chatIdRaw == null || chatIdRaw.isBlank()) {
            return false;
        }

        try {
            UUID chatId = UUID.fromString(chatIdRaw);
            attributes.put(CHAT_ID, chatId);
            return true;

        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Map<String, String> parseQuery(String query) {
        return java.util.Arrays.stream(query.split("&"))
                .map(this::splitParam)
                .filter(p -> p.key != null)
                .collect(Collectors.toMap(
                        p -> p.key,
                        p -> p.value
                ));
    }

    private Param splitParam(String param) {
        int idx = param.indexOf("=");

        if (idx == -1) {
            return new Param(null, null);
        }

        String key = decode(param.substring(0, idx));
        String value = decode(param.substring(idx + 1));

        return new Param(key, value);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record Param(String key, String value) {}

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