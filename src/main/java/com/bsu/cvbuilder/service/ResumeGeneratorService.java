package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.Resume;

import java.io.IOException;

public interface ResumeGeneratorService {

    byte[] generateResume(Resume resume) throws IOException;
}
