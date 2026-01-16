package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.annotation.limit.Limited;
import com.bsu.cvbuilder.domain.entity.resume.Resume;
import com.bsu.cvbuilder.service.ResumeGeneratorService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeGeneratorServiceImpl implements ResumeGeneratorService {

    private final TemplateEngine templateEngine;

    @Override
    @Limited(value = LimitType.RESUME_DOWNLOAD, capacity = 5)
    public byte[] generateResume(Resume resume) throws IOException {
        log.debug("Attempting to generate resume for {}", resume.getId());
        Context context = new Context();

        context.setVariable("blocks", resume.getBlocks());

        String htmlContent = templateEngine.process("/resume/%s".formatted(resume.getResumeSettings().getResumeTemplate()), context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);

            builder.useFont(new File(getClass().getClassLoader().getResource("fonts/Roboto-Regular.ttf").getFile()), "Roboto");

            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }
}
