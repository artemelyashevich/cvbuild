package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;

public interface AnalyzerService {

    String analyze(String resumeId);

    void ats(Resume resume, String jobDescription, UserProfile currentUser);
}
