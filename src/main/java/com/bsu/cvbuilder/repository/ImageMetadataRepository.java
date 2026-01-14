package com.bsu.cvbuilder.repository;

import com.bsu.cvbuilder.domain.entity.image.ImageMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ImageMetadataRepository extends MongoRepository<ImageMetadata, String> {

    List<ImageMetadata> findByOwnerId(final String ownerId);
}