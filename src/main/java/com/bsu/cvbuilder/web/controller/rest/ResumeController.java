package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.entity.resume.Resume;
import com.bsu.cvbuilder.service.ResumeGeneratorService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.web.dto.resume.UpdateResumeRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Tag(name = "Resume")
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeGeneratorService resumeGeneratorService;

    @GetMapping
    public Page<Resume> findAll(
            @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(name = "size", defaultValue = "5", required = false) Integer size
    ) {
        return resumeService.findAll(Pageable
                .ofSize(size)
                .withPage(page)
        );
    }

    @GetMapping("/{id}")
    public Resume findById(@PathVariable String id) {
        return resumeService.findById(id);
    }

    @PatchMapping("/{id}")
    public Resume update(@PathVariable String id, @RequestBody UpdateResumeRequest updateResumeRequest) {
        return resumeService.update(id, updateResumeRequest);
    }

    @GetMapping("/generate/{id}")
    public ResponseEntity<byte[]> generateResume(@PathVariable String id) throws IOException {
        var resume = resumeService.findById(id);
        var filename =  "resume.pdf";
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
