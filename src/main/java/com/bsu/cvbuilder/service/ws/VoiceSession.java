package com.bsu.cvbuilder.service.ws;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.web.socket.WebSocketSession;
import org.vosk.Recognizer;
import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-connection voice state. {@code session} must be thread-safe for sending
 * (AI tokens arrive on reactor threads while partial results are sent from the WS thread).
 * Recognizer access is synchronized because the cache may close it from another thread.
 */
@RequiredArgsConstructor
public class VoiceSession implements AutoCloseable {

    private static final String EMPTY_RESULT = "{}";

    @Getter
    private final WebSocketSession session;
    private final Recognizer recognizer;
    private final AtomicReference<Disposable> aiResponse = new AtomicReference<>();
    private final AtomicLong aiGeneration = new AtomicLong();
    private boolean closed;

    @Getter
    @Setter
    private String lastPartial = "";

    public synchronized boolean acceptWaveForm(byte[] audio, int length) {
        return !closed && recognizer.acceptWaveForm(audio, length);
    }

    public synchronized String getResult() {
        return closed ? EMPTY_RESULT : recognizer.getResult();
    }

    public synchronized String getPartialResult() {
        return closed ? EMPTY_RESULT : recognizer.getPartialResult();
    }

    public synchronized String getFinalResult() {
        return closed ? EMPTY_RESULT : recognizer.getFinalResult();
    }

    /**
     * Starts a new AI response generation, invalidating any previous or still-preparing one.
     */
    public synchronized long startAiResponse() {
        long generation = aiGeneration.incrementAndGet();
        disposeCurrent();
        return generation;
    }

    /**
     * Binds a subscription to its generation; disposes it right away if a newer one has started since.
     */
    public synchronized void bindAiResponse(long generation, Disposable response) {
        if (generation != aiGeneration.get()) {
            response.dispose();
            return;
        }
        disposeCurrent();
        aiResponse.set(response);
    }

    public boolean isCurrent(long generation) {
        return generation == aiGeneration.get();
    }

    public void cancelAiResponse() {
        startAiResponse();
    }

    private void disposeCurrent() {
        Disposable previous = aiResponse.getAndSet(null);
        if (previous != null) {
            previous.dispose();
        }
    }

    @Override
    public synchronized void close() {
        cancelAiResponse();
        if (!closed) {
            closed = true;
            recognizer.close();
        }
    }
}
