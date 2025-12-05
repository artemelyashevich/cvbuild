package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.service.LimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitSchedule {

    private final LimitService limitService;

    @Scheduled(cron = "0 0 * * * *") // every 1 HOUR
    public void job() {
        log.info("--- LIMIT JOB: STARTED ---");
        var limits = limitService.findAllLimits();

        limits.forEach(limit -> {
           var appliedFor = limit.getAppliedFor();
           var now = LocalDate.now();
           var difference = Duration.between(appliedFor, now);

           if (difference.compareTo(Duration.ofHours(12)) < 0) {
               log.debug("--- LIMIT JOB: ATTEMPTING SEND NOTIFICATION BEFORE DEACTIVATION ---");
               //TODO
           }
           if (difference.compareTo(Duration.ofDays(29)) < 0) {
               log.debug("--- LIMIT JOB: ATTEMPTING SEND NOTIFICATION AFTER DEACTIVATION ---");
               //TODO
           }
           if (difference.compareTo(Duration.ofMinutes(5)) > 0) {
               log.debug("--- LIMIT JOB: DEACTIVATION PROCESSING ---");
               // TODO
           }
        });

        log.info("--- LIMIT JOB: FINISHED ---");
    }
}
