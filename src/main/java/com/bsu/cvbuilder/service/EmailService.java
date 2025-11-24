package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.dto.EmailDto;

public interface EmailService {

    void sendEmail(EmailDto email);
}
