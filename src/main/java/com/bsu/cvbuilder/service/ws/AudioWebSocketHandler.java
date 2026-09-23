package com.bsu.cvbuilder.service.ws;

import com.bsu.cvbuilder.cache.AudioWSCache;
import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.service.flow.chat.ChatFlowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.vosk.Model;
import org.vosk.Recognizer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Executor;


@Slf4j
public class AudioWebSocketHandler extends AbstractWebSocketHandler implements AutoCloseable {

    private static final int SAMPLE_RATE = 16000;
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int SEND_BUFFER_LIMIT_BYTES = 512 * 1024;

    private final AudioWSCache cache;
    private final ChatFlowService chatFlowService;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    private final Model model;

    public AudioWebSocketHandler(AudioWSCache cache,
                                 ChatFlowService chatFlowService,
                                 ObjectMapper objectMapper,
                                 ApplicationProperties applicationProperties,
                                 Executor executor) throws IOException {

        this.cache = cache;
        this.chatFlowService = chatFlowService;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.model = new Model(applicationProperties.getVolkModel());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        WebSocketSession concurrentSession =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT_BYTES);
        cache.put(session.getId(), new VoiceSession(concurrentSession, new Recognizer(model, SAMPLE_RATE)));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        VoiceSession voice = cache.get(session.getId());
        if (voice == null || !session.isOpen()) return;

        ByteBuffer payload = message.getPayload();
        byte[] audio = new byte[payload.remaining()];
        payload.get(audio);

        if (voice.acceptWaveForm(audio, audio.length)) {
            handleFinal(voice, voice.getResult());
        } else {
            handlePartial(voice);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        VoiceSession voice = cache.get(session.getId());
        if (voice == null) return;

        switch (extractText(message.getPayload(), "type")) {
            case "stop" -> handleFinal(voice, voice.getFinalResult());
            case "cancel" -> voice.cancelAiResponse();
            default -> log.debug("Unknown voice command: {}", message.getPayload());
        }
    }

    private void handleFinal(VoiceSession voice, String resultJson) throws Exception {
        voice.setLastPartial("");
        String text = extractText(resultJson, "text");

        if (text.isBlank()) return;

        send(voice, new VoiceMessageDto("final", text));
        processAi(voice, text);
    }

    private void handlePartial(VoiceSession voice) throws Exception {
        String partial = extractText(voice.getPartialResult(), "partial");

        if (partial.isBlank() || partial.equals(voice.getLastPartial())) return;

        voice.setLastPartial(partial);
        send(voice, new VoiceMessageDto("partial", partial));
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

    private void processAi(VoiceSession voice, String text) {
        WebSocketSession session = voice.getSession();
        UUID chatId = (UUID) session.getAttributes().get(AudioHandshakeInterceptor.CHAT_ID);
        if (chatId == null || !(session.getPrincipal() instanceof Authentication authentication)) return;

        long generation = voice.startAiResponse();

        executor.execute(() -> {
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            try {
                Flux<String> answer = chatFlowService.streamMessage(chatId, text);
                if (!voice.isCurrent(generation)) return;

                SentenceSplitter splitter = new SentenceSplitter();
                Disposable response = answer
                        .subscribe(
                                chunk -> {
                                    sendSafe(voice, new AiStreamChunk("ai_chunk", chunk));
                                    splitter.accept(chunk).forEach(s -> sendSafe(voice, new AiStreamChunk("ai_sentence", s)));
                                },
                                error -> {
                                    log.error("Voice AI streaming error for chatId={}", chatId, error);
                                    sendSafe(voice, new AiStreamChunk("error", "AI response failed"));
                                },
                                () -> {
                                    splitter.flush().forEach(s -> sendSafe(voice, new AiStreamChunk("ai_sentence", s)));
                                    sendSafe(voice, new AiStreamChunk("ai_done", ""));
                                }
                        );
                voice.bindAiResponse(generation, response);
            } catch (Exception e) {
                log.error("Voice AI request failed for chatId={}", chatId, e);
                sendSafe(voice, new AiStreamChunk("error", "AI response failed"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private void send(VoiceSession voice, Object payload) throws IOException {
        WebSocketSession session = voice.getSession();
        if (!session.isOpen()) return;

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void sendSafe(VoiceSession voice, Object payload) {
        try {
            send(voice, payload);
        } catch (Exception e) {
            log.debug("Failed to send voice message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cache.remove(session.getId());
    }

    @Override
    public void close() {
        model.close();
    }

    public record VoiceMessageDto(String type, String text) {}
}
