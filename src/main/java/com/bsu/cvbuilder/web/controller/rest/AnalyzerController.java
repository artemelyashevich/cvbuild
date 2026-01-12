package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.AnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/analyzer")
@RequiredArgsConstructor
public class AnalyzerController {

    private final AnalyzerService analyzerService;

    @GetMapping("/{resumeId}")
    public String analyze(@PathVariable String resumeId) {
        return analyzerService.analyze(resumeId);
    }
}
