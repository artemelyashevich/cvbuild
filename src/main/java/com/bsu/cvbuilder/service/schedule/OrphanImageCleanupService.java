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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
public class OrphanImageCleanupService {

    private static final int BATCH_SIZE = 500;

    private final ImageMetadataRepository imageMetadataRepository;
    private final UserProfileRepository userProfileRepository;
    private final GridFsTemplate gridFsTemplate;

    @Transactional
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Monitored(value = "scheduling.orphan_image", context = "cleanup")
    public void cleanupOrphanImages() {

        log.info("[ORPHAN_IMAGE_CLEANUP] Started");

        int page = 0;
        int totalDeleted = 0;
        Page<ImageMetadata> imagePage;

        do {
            imagePage = imageMetadataRepository.findAll(PageRequest.of(page, BATCH_SIZE));
            List<ImageMetadata> images = imagePage.getContent();

            if (images.isEmpty()) {
                break;
            }

            totalDeleted += processBatch(images);
            page++;

        } while (imagePage.hasNext());

        log.info("[ORPHAN_IMAGE_CLEANUP] Finished. Total deleted: {}", totalDeleted);
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
            log.debug("Deleting orphan image: {}", image.getId());

            gridFsTemplate.delete(
                    new Query(Criteria.where("_id")
                            .is(new ObjectId(image.getId())))
            );

            imageMetadataRepository.deleteById(image.getId());

            return true;

        } catch (Exception ex) {
            log.error("Failed to delete orphan image: {}", image.getId(), ex);
            return false;
        }
    }
}
