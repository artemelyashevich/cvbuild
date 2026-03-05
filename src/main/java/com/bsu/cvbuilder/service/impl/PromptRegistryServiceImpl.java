package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.PromptRegistryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptRegistryServiceImpl implements PromptRegistryService {

    private final ResourcePatternResolver resourcePatternResolver;

    @Value("${app.ai.prompts-path:classpath:/prompt/*.txt}")
    private String promptsPath;

    @Value("${app.ai.prompts-flow-path:classpath:/prompt/flow/*.txt}")
    private String promptsFlowPath;

    private Map<String, String> prompts = Collections.emptyMap();
    private Map<String, String> flowPrompts = Collections.emptyMap();

    @PostConstruct
    public void init() {
        log.info("Loading AI prompts from {}", promptsPath);
        Map<String, String> loadedPrompts = new HashMap<>();
        Map<String, String> loadedFlowPrompts = new HashMap<>();

        try {
            Resource[] resources = resourcePatternResolver.getResources(promptsPath);
            Resource[] flowResources = resourcePatternResolver.getResources(promptsFlowPath);

            if (resources.length == 0) {
                log.warn("No prompt files found at path: {}", promptsPath);
            }

            if (flowResources.length == 0) {
                log.warn("No prompt files found at path: {}", promptsFlowPath);
            }

            process(resources, loadedPrompts);
            process(flowResources, loadedFlowPrompts);
        } catch (IOException e) {
            log.error("Failed to load prompts from {}", promptsPath, e);
            throw new AppException("Critical failure: cannot load AI prompts", e, 500);
        }

        this.prompts = Collections.unmodifiableMap(loadedPrompts);
        this.flowPrompts = Collections.unmodifiableMap(loadedFlowPrompts);
        log.info("Successfully loaded {} prompts.", prompts.size());
    }

    private void process(Resource[] flowResources, Map<String, String> flowPrompts) throws IOException {
        for (Resource resource : flowResources) {
            String filename = resource.getFilename();
            if (filename != null && filename.endsWith(".txt")) {
                String name = filename.replace(".txt", "");
                String content = resource.getContentAsString(StandardCharsets.UTF_8).trim();

                if (content.isEmpty()) {
                    log.warn("Prompt file '{}' is empty", filename);
                }

                flowPrompts.put(name, content);
                log.debug("Registered prompt: [{}]", name);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        log.debug("Destroying AI prompts from {}", promptsPath);
        prompts.clear();
        log.info("Successfully destroyed AI prompts from {}", promptsPath);
    }

    @Override
    public String getPrompt(String name) {
        return resolve(name, prompts);
    }

    @Override
    public String getFlowPrompt(String name) {
        return resolve(name, flowPrompts);
    }

    private String resolve(String name, Map<String, String> prompts) {
        return Optional.ofNullable(prompts.get(name))
                .orElseThrow(() -> {
                    log.error("Prompt requested but not found: '{}'", name);
                    return new AppException("AI Configuration error: prompt '" + name + "' is missing", 500);
                });
    }
}