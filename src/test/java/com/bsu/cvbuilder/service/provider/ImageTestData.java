package com.bsu.cvbuilder.service.provider;

import com.bsu.cvbuilder.domain.entity.ImageMetadata;
import org.bson.types.ObjectId;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public final class ImageTestData {

    private ImageTestData() {
    }

    public static final String USER_ID = "user-123";
    public static final String IMAGE_ID = new ObjectId().toString();
    public static final String FILENAME = "profile.png";
    public static final String CONTENT_TYPE = "image/png";
    public static final byte[] CONTENT = "dummy-image-content".getBytes();

    public static ImageMetadata createMetadata() {
        return ImageMetadata.builder()
                .id(IMAGE_ID)
                .filename(FILENAME)
                .contentType(CONTENT_TYPE)
                .ownerId(USER_ID)
                .build();
    }

    public static MultipartFile createMockMultipartFile() {
        return new MockMultipartFile(
                "file",
                FILENAME,
                CONTENT_TYPE,
                CONTENT
        );
    }
}