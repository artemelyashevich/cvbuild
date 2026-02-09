package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.dto.auth.NotificationDto;
import com.bsu.cvbuilder.domain.dto.auth.NotificationEngine;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.NotificationStrategy;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service("email")
@RequiredArgsConstructor
public class EmailNotificationStrategyImpl implements NotificationStrategy {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendNotification(NotificationDto notificationDto) {
        log.debug("Attempting to send email.");

        try {
            var mimeMessage = javaMailSender.createMimeMessage();
            var helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED,
                    StandardCharsets.UTF_8.name()
            );

            var context = new Context();
            context.setVariables(notificationDto.getParameters());

            helper.setFrom(from);
            helper.setTo(notificationDto.getReceiver());
            helper.setSubject("Notification from CV Builder");

            var template = templateEngine.process(notificationDto.getTemplateName(), context);

            helper.setText(template, true);

            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Error while sending email.", e);
            throw new AppException("Failed send email: %s".formatted(e.getMessage()), 500);
        }

        log.info("Email has been sent.");
    }

    @Override
    public NotificationEngine getSupportedEngine() {
        return NotificationEngine.EMAIL;
    }
}
