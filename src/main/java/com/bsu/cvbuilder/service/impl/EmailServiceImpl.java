package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.dto.EmailDto;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void sendEmail(EmailDto email) {
        log.debug("Attempting to send email.");

        try {
            var mimeMessage = javaMailSender.createMimeMessage();
            var helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED,
                    StandardCharsets.UTF_8.name()
            );

            var context = new Context();
            context.setVariables(Map.of(
                    "username", email.receiver(),
                    "activation_code", email.activationCode()
            ));

            helper.setFrom(email.sender());
            helper.setTo(email.receiver());
            helper.setSubject(email.receiver());

            var template = templateEngine.process(email.template(), context);

            helper.setText(template, true);

            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Error while sending email.", e);
            throw new AppException("Failed send email: %s".formatted(e.getMessage()), 500);
        }

        log.info("Email has been sent.");
    }
}
