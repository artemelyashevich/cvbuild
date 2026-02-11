package com.bsu.cvbuilder.controller.provider;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.stream.Stream;

public class ImageIntegrationTestData {

    public static final String IMAGE_BASE_URL = "/api/v1/images";

    /**
     * Creates a dummy multipart request body containing a "file".
     */
    public static MultiValueMap<String, Object> createMultipartRequest(String filename, byte[] content) {
        var fileResource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        var body = new LinkedMultiValueMap<String, Object>();
        body.add("file", fileResource);
        return body;
    }

    public static Stream<Arguments> uploadProvider() {
        return Stream.of(
                Arguments.of("profile.jpg", new byte[]{1, 2, 3, 4}, "user-123"),
                Arguments.of("resume-photo.png", new byte[]{5, 6, 7, 8}, "user-456")
        );
    }

    public static Stream<Arguments> genericUploadProvider() {
        return Stream.of(
                Arguments.of("test-image.jpg", new byte[]{10, 11, 12})
        );
    }
}