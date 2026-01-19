package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.domain.entity.image.ImageMetadata;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.ImageMetadataRepository;
import com.bsu.cvbuilder.service.impl.ImageServiceImpl;
import com.mongodb.CursorType;
import com.mongodb.ExplainVerbosity;
import com.mongodb.Function;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.cursor.TimeoutMode;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Collation;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private ImageMetadataRepository imageMetadataRepository;

    @InjectMocks
    private ImageServiceImpl imageService;

    private static final String TEST_USER_ID = "user123";
    private static final String TEST_FILE_ID = "507f1f77bcf86cd799439011";
    private static final String TEST_FILENAME = "test-image.jpg";

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    // ==================== findByOwnerId Tests ====================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void findByOwnerId_InvalidUserId_ReturnsEmptyList(String userId) {
        // Arrange
        when(imageMetadataRepository.findByOwnerId(userId)).thenReturn(List.of());

        // Act
        var result = imageService.findByOwnerId(userId);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty()),
                () -> verify(imageMetadataRepository).findByOwnerId(userId),
                () -> verifyNoInteractions(gridFsTemplate)
        );
    }

    @Test
    void findByOwnerId_NoMetadataFound_ReturnsEmptyList() {
        // Arrange
        when(imageMetadataRepository.findByOwnerId(TEST_USER_ID)).thenReturn(List.of());

        // Act
        var result = imageService.findByOwnerId(TEST_USER_ID);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertTrue(result.isEmpty()),
                () -> verify(imageMetadataRepository).findByOwnerId(TEST_USER_ID),
                () -> verifyNoInteractions(gridFsTemplate)
        );
    }

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.service.provider.ImageTestDataProvider#provideImageBytesData")
    void findById_ValidId_ReturnsImageBytes(byte[] expectedBytes) throws IOException {
        // Arrange
        var mockGridFsFile = mock(GridFSFile.class);
        var mockResource = mock(GridFsResource.class);
        var inputStream = new ByteArrayInputStream(expectedBytes);

        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(mockGridFsFile);
        when(gridFsTemplate.getResource(mockGridFsFile)).thenReturn(mockResource);
        when(mockResource.getInputStream()).thenReturn(inputStream);

        // Act
        var result = imageService.findById(TEST_FILE_ID);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertArrayEquals(expectedBytes, result),
                () -> verify(gridFsTemplate).findOne(any(Query.class)),
                () -> verify(gridFsTemplate).getResource(mockGridFsFile)
        );
    }

    @Test
    void findById_InputStreamThrowsIOException_ThrowsAppException() throws IOException {
        // Arrange
        var mockGridFsFile = mock(GridFSFile.class);
        var mockResource = mock(GridFsResource.class);

        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(mockGridFsFile);
        when(gridFsTemplate.getResource(mockGridFsFile)).thenReturn(mockResource);
        when(mockResource.getInputStream()).thenThrow(new IOException("Stream error"));

        // Act & Assert
        var exception = assertThrows(AppException.class, () -> imageService.findById(TEST_FILE_ID));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("Failed to read image data")),
                () -> assertEquals(500, exception.getStatusCode()),
                () -> assertNotNull(exception.getCause()),
                () -> assertTrue(exception.getCause() instanceof IOException)
        );
    }

    // ==================== create Tests ====================

    @ParameterizedTest
    @CsvSource({
            "image.jpg, image/jpeg, user1",
            "photo.png, image/png, user2",
            "document.pdf, application/pdf, user3"
    })
    void create_ValidFileAndUser_CreatesMetadata(String filename, String contentType, String userId) throws IOException {
        // Arrange
        var fileContent = "test content".getBytes();
        var multipartFile = new MockMultipartFile(
                "file",
                filename,
                contentType,
                fileContent
        );

        var objectId = new ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), eq(filename), eq(contentType)))
                .thenReturn(objectId);

        var expectedMetadata = ImageMetadata.builder()
                .id(objectId.toString())
                .filename(filename)
                .contentType(contentType)
                .ownerId(userId)
                .build();

        when(imageMetadataRepository.save(any(ImageMetadata.class))).thenReturn(expectedMetadata);

        // Act
        var result = imageService.create(multipartFile, userId);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(objectId.toString(), result.getId()),
                () -> assertEquals(filename, result.getFilename()),
                () -> assertEquals(contentType, result.getContentType()),
                () -> assertEquals(userId, result.getOwnerId()),
                () -> verify(gridFsTemplate).store(any(InputStream.class), eq(filename), eq(contentType)),
                () -> verify(imageMetadataRepository).save(any(ImageMetadata.class))
        );
    }

    // ==================== upload Tests ====================

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.service.provider.ImageTestDataProvider#provideMultipartFiles")
    void upload_ValidFile_ReturnsObjectId(MultipartFile file) throws IOException {
        // Arrange
        var objectId = new ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString()))
                .thenReturn(objectId);

        // Act
        var result = imageService.upload(file);

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(objectId.toString(), result),
                () -> verify(gridFsTemplate).store(any(InputStream.class), anyString(), anyString())
        );
    }

    @Test
    void upload_NullFile_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> imageService.upload(null));
    }

    @Test
    void upload_FileWithIOException_ThrowsAppException() throws IOException {
        // Arrange
        var multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn(TEST_FILENAME);
        when(multipartFile.getInputStream()).thenThrow(new IOException("File read error"));

        // Act & Assert
        var exception = assertThrows(AppException.class, () -> imageService.upload(multipartFile));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("Could not store file")),
                () -> assertTrue(exception.getMessage().contains(TEST_FILENAME)),
                () -> assertEquals(500, exception.getStatusCode())
        );
    }

    // ==================== Helper Methods ====================

    private com.mongodb.client.FindIterable<GridFSFile> createMockFindIterable(List<GridFSFile> files) {
        return new com.mongodb.client.FindIterable<GridFSFile>() {
            @Override
            public <A extends java.util.Collection<? super GridFSFile>> A into(A target) {
                target.addAll(files);
                return target;
            }
            @Override public com.mongodb.client.MongoCursor<GridFSFile> iterator() { throw new UnsupportedOperationException(); }
            @Override public com.mongodb.client.MongoCursor<GridFSFile> cursor() { throw new UnsupportedOperationException(); }
            @Override public GridFSFile first() { throw new UnsupportedOperationException(); }

            @Override
            public <U> MongoIterable<U> map(Function<GridFSFile, U> function) {
                return null;
            }

            @Override public com.mongodb.client.FindIterable<GridFSFile> filter(org.bson.conversions.Bson filter) { throw new UnsupportedOperationException(); }
            @Override public com.mongodb.client.FindIterable<GridFSFile> limit(int limit) { throw new UnsupportedOperationException(); }
            @Override public com.mongodb.client.FindIterable<GridFSFile> skip(int skip) { throw new UnsupportedOperationException(); }
            @Override public com.mongodb.client.FindIterable<GridFSFile> maxTime(long maxTime, java.util.concurrent.TimeUnit timeUnit) { throw new UnsupportedOperationException(); }

            @Override
            public FindIterable<GridFSFile> maxAwaitTime(long l, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> projection(Bson bson) {
                return null;
            }

            @Override public com.mongodb.client.FindIterable<GridFSFile> batchSize(int batchSize) { throw new UnsupportedOperationException(); }

            @Override
            public FindIterable<GridFSFile> collation(Collation collation) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> comment(String s) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> comment(BsonValue bsonValue) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> hint(Bson bson) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> hintString(String s) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> let(Bson bson) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> max(Bson bson) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> min(Bson bson) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> returnKey(boolean b) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> showRecordId(boolean b) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> allowDiskUse(Boolean aBoolean) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> timeoutMode(TimeoutMode timeoutMode) {
                return null;
            }

            @Override
            public Document explain() {
                return null;
            }

            @Override
            public Document explain(ExplainVerbosity explainVerbosity) {
                return null;
            }

            @Override
            public <E> E explain(Class<E> aClass) {
                return null;
            }

            @Override
            public <E> E explain(Class<E> aClass, ExplainVerbosity explainVerbosity) {
                return null;
            }

            @Override public com.mongodb.client.FindIterable<GridFSFile> noCursorTimeout(boolean noCursorTimeout) { throw new UnsupportedOperationException(); }

            @Override
            public FindIterable<GridFSFile> partial(boolean b) {
                return null;
            }

            @Override
            public FindIterable<GridFSFile> cursorType(CursorType cursorType) {
                return null;
            }

            @Override public com.mongodb.client.FindIterable<GridFSFile> sort(org.bson.conversions.Bson sort) { throw new UnsupportedOperationException(); }
            @Override public void forEach(java.util.function.Consumer<? super GridFSFile> action) { throw new UnsupportedOperationException(); }
        };
    }
}