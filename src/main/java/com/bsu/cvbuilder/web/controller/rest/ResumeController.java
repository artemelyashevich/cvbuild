package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.agreement.AgreementRequire;
import com.bsu.cvbuilder.annotation.email.EmailVerification;
import com.bsu.cvbuilder.service.AtsService;
import com.bsu.cvbuilder.service.ResumeGeneratorService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.web.dto.resume.ResumeResponseDto;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import com.bsu.cvbuilder.web.mapper.impl.ResumeResponseDtoMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Resume")
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeGeneratorService resumeGeneratorService;
    private final AtsService atsService;
    private final ResumeResponseDtoMapper resumeResponseDtoMapper;

    @AgreementRequire
    @EmailVerification
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/ats/{resumeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void ats(@PathVariable String resumeId, @RequestBody Map<String, String> body) {
        atsService.optimizeAsync(resumeId, body.get("url"));
    }

    @GetMapping
    public Page<ResumeResponseDto> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(name = "size", defaultValue = "5", required = false) Integer size,
            @RequestParam(name = "masked", defaultValue = "false", required = false) Boolean masked
    ) {
        return resumeService.findAll(Pageable
                        .ofSize(size)
                        .withPage(page))
                .map(masked ? resumeResponseDtoMapper::toMaskedDto : resumeResponseDtoMapper::toDto);
    }

    @PostMapping("/{chatId}")
    public void generateFromChat(@PathVariable String chatId) {
        resumeService.findByChatId(UUID.fromString(chatId));
    }

    @GetMapping("/{id}")
    public ResumeResponseDto findById(@PathVariable String id) {
        return resumeResponseDtoMapper.toDto(resumeService.findById(id));
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponseDto update(@PathVariable String id, @RequestBody UpdateResumeRequest updateResumeRequest) {
        return resumeResponseDtoMapper.toDto(resumeService.update(id, updateResumeRequest));
    }

    @GetMapping("/generate/{id}")
    public ResponseEntity<byte[]> generateResume(@PathVariable String id) throws IOException {
        var resume = resumeService.findById(id);
        var filename = "resume.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(resumeGeneratorService.generateResume(resume));
    }
}