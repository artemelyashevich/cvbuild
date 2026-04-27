package com.bsu.cvbuilder.service.schedule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractScheduler {

    private Boolean isEnable = true;

    protected static final String LOG_PREFIX = "[JOB]";

    protected final void execute(String jobName, Runnable executeJob) {

        long start = System.currentTimeMillis();
        String job = jobName.toLowerCase();

        try {
            executeJob.run();

            long duration = System.currentTimeMillis() - start;
            if (Boolean.TRUE.equals(isEnable)) {
                log.debug("{} {{}} status=SUCCESS durationMs={}",
                        LOG_PREFIX, job, duration);
            }


        } catch (Exception ex) {

            long duration = System.currentTimeMillis() - start;

            log.error("{} {{}} status=FAILED durationMs={}",
                    LOG_PREFIX, job, duration, ex);
        }
    }

    protected void setEnabled(boolean enabled) {
        isEnable = enabled;
    }
}