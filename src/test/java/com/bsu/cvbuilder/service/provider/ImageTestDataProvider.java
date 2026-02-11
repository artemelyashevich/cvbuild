package com.bsu.cvbuilder.service.provider;

import com.bsu.cvbuilder.domain.entity.ImageMetadata;
import org.bson.types.ObjectId;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Stream;

/**
 * Test data provider for ImageServiceImpl tests.
 * Contains reusable data sources for parameterized tests.
 */
public final class ImageTestDataProvider {

    private ImageTestDataProvider() {
        // Utility class - prevent instantiation
    }

    /**
     * Provides various lists of ImageMetadata for testing findByOwnerId scenarios.
     *
     * @return Stream of Arguments containing metadata lists and expected counts
     */
    public static Stream<Arguments> provideImageMetadataLists() {
        return Stream.of(
                // Single metadata entry
                Arguments.of(
                        List.of(createImageMetadata("507f1f77bcf86cd799439011", "image1.jpg", "user123")),
                        1
                ),
                // Multiple metadata entries
                Arguments.of(
                        List.of(
                                createImageMetadata("507f1f77bcf86cd799439012", "image1.jpg", "user123"),
                                createImageMetadata("507f1f77bcf86cd799439013", "image2.png", "user123"),
                                createImageMetadata("507f1f77bcf86cd799439014", "image3.gif", "user123")
                        ),
                        3
                ),
                // Large dataset
                Arguments.of(
                        List.of(
                                createImageMetadata("507f1f77bcf86cd799439015", "photo1.jpg", "user123"),
                                createImageMetadata("507f1f77bcf86cd799439016", "photo2.jpg", "user123"),
                                createImageMetadata("507f1f77bcf86cd799439017", "photo3.jpg", "user123"),
                                createImageMetadata("507f1f77bcf86cd799439018", "photo4.jpg", "user123"),
                                createImageMetadata("507f1f77bcf86cd799439019", "photo5.jpg", "user123")
                        ),
                        5
                )
        );
    }

    /**
     * Provides various byte arrays representing image data.
     *
     * @return Stream of byte arrays for testing image retrieval
     */
    public static Stream<byte[]> provideImageBytesData() {
        return Stream.of(
                // Empty image
                new byte[0],
                // Small image
                "small image content".getBytes(),
                // Medium sized image
                createByteArray(1024), // 1KB
                // Larger image
                createByteArray(10240) // 10KB
        );
    }

    /**
     * Provides various MultipartFile instances for upload testing.
     *
     * @return Stream of MultipartFile instances
     */
    public static Stream<MultipartFile> provideMultipartFiles() {
        return Stream.of(
                new MockMultipartFile(
                        "file",
                        "image.jpg",
                        "image/jpeg",
                        "jpeg image content".getBytes()
                ),
                new MockMultipartFile(
                        "file",
                        "photo.png",
                        "image/png",
                        "png image content".getBytes()
                ),
                new MockMultipartFile(
                        "file",
                        "document.pdf",
                        "application/pdf",
                        "pdf document content".getBytes()
                ),
                new MockMultipartFile(
                        "file",
                        "animation.gif",
                        "image/gif",
                        createByteArray(5000) // 5KB GIF
                )
        );
    }

    /**
     * Provides invalid file scenarios for negative testing.
     *
     * @return Stream of Arguments containing invalid files and expected error messages
     */
    public static Stream<Arguments> provideInvalidFiles() {
        return Stream.of(
                Arguments.of(
                        new MockMultipartFile("file", "", "text/plain", new byte[0]),
                        "empty filename"
                ),
                Arguments.of(
                        new MockMultipartFile("file", "test.txt", null, "content".getBytes()),
                        "null content type"
                ),
                Arguments.of(
                        new MockMultipartFile("file", "test.dat", "application/octet-stream", new byte[0]),
                        "empty content"
                )
        );
    }

    /**
     * Provides various user IDs for testing.
     *
     * @return Stream of user ID strings
     */
    public static Stream<String> provideUserIds() {
        return Stream.of(
                "user123",
                "user-with-dashes",
                "user_with_underscores",
                "user@with@symbols",
                "123456789",
                new ObjectId().toString()
        );
    }

    /**
     * Provides various file IDs (ObjectIds) for testing.
     *
     * @return Stream of file ID strings
     */
    public static Stream<String> provideFileIds() {
        return Stream.of(
                "507f1f77bcf86cd799439011",
                "507f191e810c19729de860ea",
                new ObjectId().toString(),
                new ObjectId().toString(),
                new ObjectId().toString()
        );
    }

    /**
     * Provides combinations of filename and content type for testing.
     *
     * @return Stream of Arguments containing filename and content type pairs
     */
    public static Stream<Arguments> provideFilenameContentTypePairs() {
        return Stream.of(
                Arguments.of("image.jpg", "image/jpeg"),
                Arguments.of("photo.png", "image/png"),
                Arguments.of("animation.gif", "image/gif"),
                Arguments.of("vector.svg", "image/svg+xml"),
                Arguments.of("bitmap.bmp", "image/bmp"),
                Arguments.of("icon.ico", "image/x-icon"),
                Arguments.of("document.pdf", "application/pdf")
        );
    }

    // ==================== Helper Methods ====================

    /**
     * Creates an ImageMetadata instance with the given parameters.
     *
     * @param id       The image ID
     * @param filename The filename
     * @param ownerId  The owner user ID
     * @return ImageMetadata instance
     */
    private static ImageMetadata createImageMetadata(String id, String filename, String ownerId) {
        return ImageMetadata.builder()
                .id(id)
                .filename(filename)
                .contentType(inferContentType(filename))
                .ownerId(ownerId)
                .build();
    }

    /**
     * Infers content type from filename extension.
     *
     * @param filename The filename
     * @return Content type string
     */
    private static String inferContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }

        var lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFilename.endsWith(".svg")) {
            return "image/svg+xml";
        }

        return "application/octet-stream";
    }

    /**
     * Creates a byte array of specified size filled with test data.
     *
     * @param size The size of the byte array
     * @return Byte array filled with test pattern
     */
    private static byte[] createByteArray(int size) {
        var bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (i % 256);
        }
        return bytes;
    }
}