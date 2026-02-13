package com.bsu.cvbuilder.service.schedule;

import com.bsu.cvbuilder.domain.entity.ImageMetadata;
import com.bsu.cvbuilder.repository.ImageMetadataRepository;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanImageCleanupService {

    private final ImageMetadataRepository imageMetadataRepository;
    private final UserProfileRepository userProfileRepository;
    private final GridFsTemplate gridFsTemplate;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void cleanupOrphanImages() {

        log.info("[CLEAN UP JOB]: Starting orphan images cleanup...");

        int deletedCount = 0;

        List<ImageMetadata> allImages = imageMetadataRepository.findAll();

        for (ImageMetadata image : allImages) {

            boolean hasOwner = image.getOwnerId() != null && userProfileRepository.existsById(image.getOwnerId());

            if (!hasOwner) {
                deleteImage(image);
                deletedCount++;
            }
        }

        log.info("[CLEAN UP JOB]: Orphan images cleanup finished. Deleted: {}", deletedCount);
    }

    private void deleteImage(ImageMetadata image) {
        try {
            log.info("Deleting orphan image: {}", image.getId());

            gridFsTemplate.delete(
                    new Query(Criteria.where("_id")
                            .is(new ObjectId(image.getId())))
            );
            imageMetadataRepository.deleteById(image.getId());

        } catch (Exception e) {
            log.error("Failed to delete orphan image: {}", image.getId(), e);
        }
    }
}
