package com.bsu.cvbuilder.service.ws;

import com.bsu.cvbuilder.cache.AudioWSCache;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.service.flow.chat.ChatFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;
import java.util.UUID;

public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    private final AudioWSCache cache;
    private final ChatFlowService chatFlowService;
    private final ObjectMapper objectMapper;

    private final Model model;

    public AudioWebSocketHandler(AudioWSCache cache,
                                 ChatFlowService chatFlowService,
                                 ObjectMapper objectMapper,
                                 ApplicationProperties applicationProperties) throws IOException {

        this.cache = cache;
        this.chatFlowService = chatFlowService;
        this.objectMapper = objectMapper;
        this.model = new Model(applicationProperties.getVolkModel());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Recognizer recognizer = new Recognizer(model, 16000);
        cache.put(session.getId(), recognizer);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {

        Recognizer recognizer = cache.get(session.getId());
        if (recognizer == null || !session.isOpen()) return;

        byte[] audio = message.getPayload().array();

        boolean finalResult = recognizer.acceptWaveForm(audio, audio.length);

        if (finalResult) {
            handleFinal(session, recognizer);
        } else {
            handlePartial(session, recognizer);
        }
    }

    private void handleFinal(WebSocketSession session, Recognizer recognizer) throws Exception {
        String text = extractText(recognizer.getResult(), "text");

        if (text.isBlank()) return;

        send(session, new VoiceMessageDto("final", text));
        processAi(session, text);
    }

    private void handlePartial(WebSocketSession session, Recognizer recognizer) throws Exception {
        String partial = extractText(recognizer.getPartialResult(), "partial");

        if (partial.isBlank()) return;

        send(session, new VoiceMessageDto("partial", partial));
    }

    private String extractText(String json, String field) throws Exception {
        return objectMapper.readTree(json)
                .path(field)
                .asText("");
    }

    record AiStreamChunk(
            String type,
            String message
    ) {
    }

    private void processAi(WebSocketSession session, String text) {
        Object chatIdObj = session.getAttributes().get("chatId");
        if (chatIdObj == null) return;

        UUID chatId;
        try {
            chatId = UUID.fromString(chatIdObj.toString());
        } catch (Exception e) {
            return;
        }

        String message = chatFlowService.processMessageSync(chatId, text);

        sendSafe(session, new AiStreamChunk("ai_chunk", message));
        sendSafe(session, new AiStreamChunk("ai_done", ""));
    }

    private void send(WebSocketSession session, Object payload) throws IOException {
        if (!session.isOpen()) return;

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void sendSafe(WebSocketSession session, Object payload) {
        try {
            send(session, payload);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cache.remove(session.getId());
    }

    public record VoiceMessageDto(String type, String text) {}
}