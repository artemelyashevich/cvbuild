package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.AnalyzerService;
import com.bsu.cvbuilder.service.JobParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analyzer")
@RequiredArgsConstructor
public class AnalyzerController {

    private final AnalyzerService analyzerService;
    private final JobParserService jobParserService;

    @GetMapping("/{resumeId}")
    public String analyze(@PathVariable String resumeId) {
        return analyzerService.analyze(resumeId);
    }

    @GetMapping("/job")
    public String job(@RequestBody Map<String, String> url) {
        return jobParserService.parse(url.get("url"));
    }
}
