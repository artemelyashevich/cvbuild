package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.resume.ResumeData;

import java.util.UUID;

public interface ResumeService {

    ResumeData extract(UUID chatId);
}
