package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.limit.LimitType;
import com.bsu.cvbuilder.annotation.limit.Limited;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.ResumeGeneratorService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeGeneratorServiceImpl implements ResumeGeneratorService {

    private final TemplateEngine templateEngine;

    private static final String FONT_PATH = "fonts/Roboto-Regular.ttf";
    private static final String FONT_FAMILY = "Roboto";

    @Override
    @Limited(value = LimitType.RESUME_DOWNLOAD, capacity = 5)
    public byte[] generateResume(Resume resume) {
        log.debug("Starting PDF generation for resume: {}", resume.getId());

        String templateName = Optional.ofNullable(resume.getResumeSettings())
                .map(Resume.ResumeSettings::getResumeTemplate)
                .orElse("default");

        Context context = new Context();
        context.setVariable("blocks", resume.getBlocks());

        String templatePath = "resume/" + templateName;
        String htmlContent;

        try {
            htmlContent = templateEngine.process(templatePath, context);
        } catch (Exception e) {
            log.error("Thymeleaf processing failed for template: {}", templatePath, e);
            throw new AppException("Failed to process resume template", 500);
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, "/");
            loadFont(builder);

            builder.toStream(os);
            builder.run();

            log.info("Resume PDF generated successfully, size: {} bytes", os.size());
            return os.toByteArray();
        } catch (Exception e) {
            log.error("PDF rendering failed for resume: {}", resume.getId(), e);
            throw new AppException("Failed to generate PDF document", 500);
        }
    }

    private void loadFont(PdfRendererBuilder builder) {
        try {
            ClassPathResource fontResource = new ClassPathResource(FONT_PATH);
            if (!fontResource.exists()) {
                log.warn("Font file not found: {}. PDF might have encoding issues.", FONT_PATH);
                return;
            }

            builder.useFont(() -> {
                try {
                    return fontResource.getInputStream();
                } catch (IOException e) {
                    throw new AppException(e, 500);
                }
            }, FONT_FAMILY);

        } catch (Exception e) {
            log.error("Could not load font: {}", FONT_PATH, e);
        }
    }
}