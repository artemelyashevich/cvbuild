package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.service.PromptRegistryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PromptRegistryServiceImpl implements PromptRegistryService {

    private final ResourcePatternResolver resourcePatternResolver;

    private final Map<String, String> prompts = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws IOException {
        Resource[] resources = resourcePatternResolver.getResources("classpath:/prompt/*.txt");
        for (Resource resource : resources) {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            String name = Objects.requireNonNull(resource.getFilename()).replace(".txt", "");
            prompts.put(name, content);
        }
    }

    @Override
    public String getPrompt(String name) {
        return prompts.getOrDefault(name, "Prompt not found: " + name);
    }
}
