package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.AnalyzerService;
import com.bsu.cvbuilder.service.JobParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analyzer")
@RequiredArgsConstructor
@Tag(name = "Analyzer", description = "Resume analysis and job parsing operations")
public class AnalyzerController {

    private final AnalyzerService analyzerService;
    private final JobParserService jobParserService;

    @GetMapping("/{resumeId}")
    @Operation(
            summary = "Analyze a resume",
            description = "Performs detailed analysis on a specific resume by its ID, providing compatibility scores and recommendations"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resume successfully analyzed"),
            @ApiResponse(responseCode = "400", description = "Invalid resume ID provided"),
            @ApiResponse(responseCode = "404", description = "Resume not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error during analysis")
    })
    public String analyze(
            @Parameter(description = "Unique identifier of the resume to analyze", required = true)
            @PathVariable String resumeId
    ) {
        return analyzerService.analyze(resumeId);
    }

    @GetMapping("/job")
    @Operation(
            summary = "Parse job description from URL",
            description = "Fetches and parses job description from the provided URL, extracting key requirements and qualifications"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job description successfully parsed"),
            @ApiResponse(responseCode = "400", description = "Invalid URL or missing URL parameter"),
            @ApiResponse(responseCode = "422", description = "Unable to parse job description from the provided URL"),
            @ApiResponse(responseCode = "500", description = "Internal server error during parsing")
    })
    public String job(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request body containing the job posting URL", required = true)
            @RequestBody Map<String, String> url
    ) {
        return jobParserService.parse(url.get("url"));
    }
}