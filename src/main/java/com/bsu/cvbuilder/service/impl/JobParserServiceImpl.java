package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.JobParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobParserServiceImpl implements JobParserService {

    private final ApplicationProperties applicationProperties;

    @Override
    public String parse(String url) {
        boolean isTrustPath = applicationProperties.getAnalyzer().getTrustUrls().stream().anyMatch(url::contains);
        if (!isTrustPath) {
            throw new AppException(
                    "This is no trust url, there are trust urls: %s".formatted(
                            String.join(", ", applicationProperties.getAnalyzer().getTrustUrls())
                    ), 400
            );
        }
        StringBuilder builder = new StringBuilder();
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(applicationProperties.getAnalyzer().getUserAgent())
                    .get();

            Elements jobCards = doc.select(".vacancy-description");
            for (Element card : jobCards) {
                builder.append(card.text());
            }
        } catch (IOException e) {
            throw new AppException("Failed to parse job url: " + url, 500);
        }
        return builder.toString();
    }
}
