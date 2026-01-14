package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.dto.EmailDto;

public interface EmailService {

    void sendEmail(EmailDto email);
}
