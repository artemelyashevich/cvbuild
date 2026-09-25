package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.Resume;

/**
 * Adapts a resume to a vacancy: parse the job link, expand the description with AI, store an ATS-optimized copy.
 */
public interface AtsService {

    /**
     * Runs the optimization in the caller thread. The original resume gets {@code atsId} of the stored copy.
     */
    Resume optimize(Resume resume, String jobLink);

    /**
     * Checks access and parses the job link in the caller thread, then optimizes in the background.
     * The outcome is reported to the user via notifications.
     */
    void optimizeAsync(String resumeId, String jobLink);
}
