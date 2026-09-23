package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.service.ws.AudioHandshakeInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.server.ServerHttpRequest;

import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudioHandshakeInterceptorTest {

    private final AudioHandshakeInterceptor interceptor = new AudioHandshakeInterceptor();

    private static ServerHttpRequest request(String query, boolean authenticated) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("ws://localhost/audio" + query));
        when(request.getPrincipal()).thenReturn(authenticated ? mock(Principal.class) : null);
        return request;
    }

    @Test
    @DisplayName("beforeHandshake: valid chatId is stored, duplicate params take the first value")
    void beforeHandshake_ValidChatId_StoresUuid() {
        UUID chatId = UUID.randomUUID();
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                request("?chatId=" + chatId + "&chatId=other&x", true), null, null, attributes);

        assertTrue(result);
        assertEquals(chatId, attributes.get(AudioHandshakeInterceptor.CHAT_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?chatId=", "?chatId=not-a-uuid", "?other=1"})
    @DisplayName("beforeHandshake: missing or invalid chatId is rejected")
    void beforeHandshake_InvalidChatId_Rejects(String query) {
        assertFalse(interceptor.beforeHandshake(request(query, true), null, null, new HashMap<>()));
    }

    @Test
    @DisplayName("beforeHandshake: unauthenticated request is rejected")
    void beforeHandshake_NoPrincipal_Rejects() {
        assertFalse(interceptor.beforeHandshake(
                request("?chatId=" + UUID.randomUUID(), false), null, null, new HashMap<>()));
    }
}
