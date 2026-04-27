package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.domain.entity.ImageMetadata;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.repository.ImageMetadataRepository;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanImageCleanupService extends AbstractScheduler {

    private static final int BATCH_SIZE = 500;
    private static final String JOB = "orphan-image-cleanup";

    private final ImageMetadataRepository imageMetadataRepository;
    private final UserProfileRepository userProfileRepository;
    private final GridFsTemplate gridFsTemplate;

    @Transactional
    @Scheduled(fixedDelay = 5 * 60 * 1000, scheduler = "cleanUpExecutor")
    @Monitored(value = "scheduling.orphan_image", context = "cleanup")
    public void cleanupOrphanImages() {

        execute(JOB, () -> {

            long start = System.currentTimeMillis();

            int page = 0;
            int totalDeleted = 0;
            int totalProcessed = 0;

            log.info("{} {cleanup} started batchSize={}", LOG_PREFIX, BATCH_SIZE);

            Page<ImageMetadata> imagePage;

            do {
                imagePage = imageMetadataRepository.findAll(PageRequest.of(page, BATCH_SIZE));
                List<ImageMetadata> images = imagePage.getContent();

                if (images.isEmpty()) {
                    break;
                }

                int deleted = processBatch(images);

                totalDeleted += deleted;
                totalProcessed += images.size();

                log.info("{} {cleanup} page={} processed={} deleted={}",
                        LOG_PREFIX, page, images.size(), deleted);

                page++;

            } while (imagePage.hasNext());

            long duration = System.currentTimeMillis() - start;

            log.info("{} {cleanup} finished processed={} deleted={} pages={} durationMs={}",
                    LOG_PREFIX,
                    totalProcessed,
                    totalDeleted,
                    page,
                    duration);
        });
    }

    private int processBatch(List<ImageMetadata> images) {

        Set<String> ownerIds = images.stream()
                .map(ImageMetadata::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> existingUsers = userProfileRepository.findAllById(ownerIds)
                .stream()
                .map(UserProfile::getId)
                .collect(Collectors.toSet());

        int deletedCount = 0;

        for (ImageMetadata image : images) {

            boolean isOrphan = image.getOwnerId() == null ||
                    !existingUsers.contains(image.getOwnerId());

            if (isOrphan && deleteImageSafely(image)) {
                deletedCount++;
            }
        }

        return deletedCount;
    }

    private boolean deleteImageSafely(ImageMetadata image) {

        try {
            gridFsTemplate.delete(
                    new org.springframework.data.mongodb.core.query.Query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id")
                                    .is(new ObjectId(image.getId()))
                    )
            );

            imageMetadataRepository.deleteById(image.getId());

            log.debug("{} {delete} orphan image removed imageId={}",
                    LOG_PREFIX, image.getId());

            return true;

        } catch (Exception ex) {
            log.error("{} {delete} failed imageId={}",
                    LOG_PREFIX, image.getId(), ex);
            return false;
        }
    }
}