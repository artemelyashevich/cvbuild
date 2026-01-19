package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.configuration.ApplicationProperties;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.JobParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobParserServiceImpl implements JobParserService {

    private final ApplicationProperties applicationProperties;

    private static final List<String> COMMON_SELECTORS = List.of(
            ".vacancy-description",
            ".job-description",
            "[data-qa=vacancy-description]",
            ".description__text",
            "section.description"
    );

    @Override
    public String parse(String url) {
        validateUrl(url);

        try {
            log.debug("Connecting to URL: {}", url);

            Document doc = Jsoup.connect(url)
                    .userAgent(applicationProperties.getAnalyzer().getUserAgent())
                    .timeout(10000) // 10 s
                    .followRedirects(true)
                    .get();

            String description = extractDescription(doc);

            if (description.isBlank()) {
                log.warn("Could not find job description using known selectors for URL: {}", url);
                description = doc.body().text();
            }

            log.info("Successfully parsed job description from: {}, length: {}", url, description.length());
            return description;

        } catch (IOException e) {
            log.error("Error fetching URL {}: {}", url, e.getMessage());
            throw new AppException("Failed to connect or parse job URL: " + url, 500);
        }
    }

    private void validateUrl(String urlString) {
        try {
            String host = URI.create(urlString).getHost();
            if (host == null) {
                throw new AppException("Invalid URL: " + urlString, 400);
            }

            List<String> trustDomains = applicationProperties.getAnalyzer().getTrustUrls();
            boolean isTrusted = trustDomains.stream().anyMatch(host::endsWith);

            if (!isTrusted) {
                log.warn("Untrusted URL attempt: {}", urlString);
                throw new AppException("Domain " + host + " is not in the trust list. Trusted: " +
                        String.join(", ", trustDomains), 400);
            }
        } catch (Exception e) {
            if (e instanceof AppException ex) {
                throw ex;
            }
            throw new AppException("Invalid URL format: " + urlString, 400);
        }
    }

    private String extractDescription(Document doc) {
        StringBuilder builder = new StringBuilder();

        for (String selector : COMMON_SELECTORS) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                elements.forEach(el -> builder.append(el.text()).append("\n"));
                break;
            }
        }

        return builder.toString().trim();
    }
}