package com.bsu.cvbuilder.service.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureDataCleanupScheduler {

    @Scheduled(cron = "0 */5 * * * *")
    public void secureDataCleanup() {

    }
}
