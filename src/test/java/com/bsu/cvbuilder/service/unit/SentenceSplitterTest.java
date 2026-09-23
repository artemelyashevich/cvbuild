package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.service.ws.SentenceSplitter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentenceSplitterTest {

    @Test
    @DisplayName("accept: emits sentences only when complete, across chunk boundaries")
    void accept_ChunkedTokens_EmitsCompleteSentences() {
        var splitter = new SentenceSplitter();
        List<String> sentences = new ArrayList<>();

        for (String chunk : List.of("Привет", "! Как", " вас зовут", "? Расскажите о", " себе")) {
            sentences.addAll(splitter.accept(chunk));
        }

        assertEquals(List.of("Привет!", "Как вас зовут?"), sentences);
        assertEquals(List.of("Расскажите о себе"), splitter.flush());
    }

    @Test
    @DisplayName("accept: does not split decimals and splits on new lines")
    void accept_DecimalAndNewLine_SplitsCorrectly() {
        var splitter = new SentenceSplitter();

        List<String> sentences = splitter.accept("Опыт 2.5 года\nДальше");

        assertEquals(List.of("Опыт 2.5 года"), sentences);
        assertEquals(List.of("Дальше"), splitter.flush());
    }

    @Test
    @DisplayName("flush: strips markdown and completion marker, skips unspeakable text")
    void flush_MarkdownAndMarker_ReturnsCleanText() {
        var splitter = new SentenceSplitter();

        List<String> sentences = splitter.accept("**Отлично!** Готово.\n**INTERVIEW_COMPLETED**\n### \n");

        assertEquals(List.of("Отлично!", "Готово."), sentences);
        assertTrue(splitter.flush().isEmpty());
    }
}
