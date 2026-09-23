package com.bsu.cvbuilder.service.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SentenceSplitter {

    private static final Pattern NOT_SPEAKABLE = Pattern.compile("\\*{0,2}#*INTERVIEW_COMPLETED#*\\*{0,2}|[*#`_]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final StringBuilder buffer = new StringBuilder();

    public List<String> accept(String chunk) {
        buffer.append(chunk);
        List<String> sentences = new ArrayList<>();

        int start = 0;
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (c == '\n') {
                addIfSpeakable(sentences, buffer.substring(start, i + 1));
                start = i + 1;
            } else if (isSentenceEnd(c)) {
                int end = i + 1;
                while (end < buffer.length() && isCloser(buffer.charAt(end))) {
                    end++;
                }
                if (end < buffer.length() && Character.isWhitespace(buffer.charAt(end))) {
                    addIfSpeakable(sentences, buffer.substring(start, end));
                    start = end;
                    i = end - 1;
                }
            }
        }
        buffer.delete(0, start);

        return sentences;
    }

    public List<String> flush() {
        List<String> sentences = new ArrayList<>();
        addIfSpeakable(sentences, buffer.toString());
        buffer.setLength(0);
        return sentences;
    }

    private static boolean isSentenceEnd(char c) {
        return c == '.' || c == '!' || c == '?' || c == '…';
    }

    private static boolean isCloser(char c) {
        return c == '*' || c == '_' || c == '"' || c == '»' || c == ')';
    }

    private static void addIfSpeakable(List<String> sentences, String raw) {
        String text = WHITESPACE.matcher(NOT_SPEAKABLE.matcher(raw).replaceAll("")).replaceAll(" ").strip();
        if (text.chars().anyMatch(Character::isLetterOrDigit)) {
            sentences.add(text);
        }
    }
}
