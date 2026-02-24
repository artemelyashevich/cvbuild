package com.bsu.cvbuilder.controller.integration;

import com.bsu.cvbuilder.AbstractTest;
import com.bsu.cvbuilder.configuration.TestSecurityConfig;
import com.bsu.cvbuilder.controller.provider.ImageIntegrationTestData;
import com.bsu.cvbuilder.service.ImageService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled
@Import(TestSecurityConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImageControllerIntegrationTest extends AbstractTest {

    @MockitoBean
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        register(null);
    }

    @Test
    @DisplayName("GET /api/v1/images: Should return list of files")
    void findAll_ImagesExist_Returns200AndList() {
        // Arrange
        when(imageService.findAll()).thenReturn(Collections.emptyList());

        // Act
        var response = restTemplate.getForEntity("http://localhost:" + port + ImageIntegrationTestData.IMAGE_BASE_URL, List.class);

        // Assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull()
        );
    }

    @Test
    @DisplayName("GET /api/v1/images/{id}: Should return bytes and correct Content-Type")
    void findById_ImageExists_ReturnsBytesAndJpegHeader() {
        // Arrange
        var imageId = "507f1f77bcf86cd799439011";
        var mockBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}; // JPEG magic numbers
        when(imageService.findById(imageId)).thenReturn(mockBytes);

        // Act
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + ImageIntegrationTestData.IMAGE_BASE_URL + "/" + imageId,
                byte[].class
        );

        // Assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_JPEG),
                () -> assertThat(response.getBody()).isEqualTo(mockBytes)
        );
    }

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.ImageIntegrationTestData#uploadProvider")
    @DisplayName("POST /api/v1/images/{userId}: Should upload file and return 201 with Location")
    void create_WithUserId_ReturnsCreatedStatus(String filename, byte[] content, String userId) {
        // Arrange
        var mockId = new ObjectId().toHexString();
        var mockFile = mock(com.mongodb.client.gridfs.model.GridFSFile.class);
        when(mockFile.getId()).thenReturn(new org.bson.BsonString(mockId));

        when(imageService.create(any(MultipartFile.class), eq(userId)));

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var requestEntity = new HttpEntity<>(
                ImageIntegrationTestData.createMultipartRequest(filename, content),
                headers
        );

        // Act
        var response = restTemplate.postForEntity(
                "http://localhost:" + port + ImageIntegrationTestData.IMAGE_BASE_URL + "/" + userId,
                requestEntity,
                String.class
        );

        // Assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isEqualTo(mockId),
                () -> assertThat(response.getHeaders().getLocation()).isNotNull(),
                () -> assertThat(response.getHeaders().getLocation().getPath()).contains(mockId)
        );
    }

    @ParameterizedTest
    @MethodSource("com.bsu.cvbuilder.controller.provider.ImageIntegrationTestData#genericUploadProvider")
    @DisplayName("POST /api/v1/images: Should upload file and return 201 Created")
    void save_GenericUpload_ReturnsCreatedStatus(String filename, byte[] content) {
        // Arrange
        var mockId = "generated-id-123";
        when(imageService.upload(any(MultipartFile.class))).thenReturn(mockId);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var requestEntity = new HttpEntity<>(
                "http://localhost:" + port + ImageIntegrationTestData.createMultipartRequest(filename, content),
                headers
        );

        // Act
        var response = restTemplate.postForEntity(
                ImageIntegrationTestData.IMAGE_BASE_URL,
                requestEntity,
                String.class
        );

        // Assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isEqualTo(mockId)
        );
    }
}