package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.resume.Resume;

import java.util.UUID;

public interface ResumeService {

    Resume findByChatId(UUID chatId);

    Resume findById(String id);
}
