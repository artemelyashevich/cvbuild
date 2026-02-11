package com.bsu.cvbuilder.service.integration;

import com.bsu.cvbuilder.AbstractTest;
import com.bsu.cvbuilder.repository.ImageMetadataRepository;
import com.bsu.cvbuilder.service.impl.ImageServiceImpl;
import com.bsu.cvbuilder.service.provider.ImageTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import static org.junit.jupiter.api.Assertions.*;

class ImageServiceImplIntegrationTest extends AbstractTest {

    @Autowired
    private ImageServiceImpl imageService;

    @Autowired
    private ImageMetadataRepository imageMetadataRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Test
    @DisplayName("createAndFind_EndToEnd_SuccessfullyStoresAndRetrieves")
    void createAndFind_EndToEnd_SuccessfullyStoresAndRetrieves() {
        // Arrange
        var file = ImageTestData.createMockMultipartFile();
        var userId = "integration-user";

        // Act - Store
        var savedMetadata = imageService.create(file, userId);

        // Act - Find by User
        var userFiles = imageService.findByOwnerId(userId);

        // Act - Download Bytes
        var downloadedBytes = imageService.findById(savedMetadata.getId());

        // Assert
        assertAll(
                () -> assertEquals(1, userFiles.size(), "User should have exactly 1 file"),
                () -> assertEquals(file.getOriginalFilename(), savedMetadata.getFilename()),
                () -> assertArrayEquals(ImageTestData.CONTENT, downloadedBytes, "Content should match original"),
                () -> assertTrue(imageMetadataRepository.existsById(savedMetadata.getId()))
        );
    }
}