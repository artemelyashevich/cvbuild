package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.ResumeData;

import java.util.UUID;

public interface ResumeDataExtractorService {

    ResumeData extract(UUID chatId);
}
