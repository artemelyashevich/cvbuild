package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.domain.entity.History;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryCleanupScheduler {

    private final MongoTemplate mongoTemplate;

    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanupOldHistoryEntries() {
        log.info("Starting scheduled history cleanup...");

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(3);

        Query query = new Query();
        query.addCriteria(Criteria.where("createdAt").lt(thresholdDate));

        query.addCriteria(Criteria.where("events").exists(true).andOperator(
                new Criteria().and("{ $gt: [ { $size: { $objectToArray: '$events' } }, 10 ] }")
        ));

        long deletedCount = mongoTemplate.remove(query, History.class).getDeletedCount();

        if (deletedCount > 0) {
            log.info("Cleanup finished. Removed {} old history records.", deletedCount);
        } else {
            log.info("Cleanup finished. No records matched criteria.");
        }
    }
}