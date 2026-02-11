package com.bsu.cvbuilder.service.unit;

import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.service.impl.PromptRegistryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptRegistryServiceImplTest {

    @Mock
    private ResourcePatternResolver resourcePatternResolver;

    @InjectMocks
    private PromptRegistryServiceImpl promptRegistryService;

    private static final String PROMPTS_PATH = "classpath:/prompt/*.txt";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(promptRegistryService, "promptsPath", PROMPTS_PATH);
    }

    // --- init Tests ---

    @Test
    @DisplayName("init: should load multiple prompts from resources successfully")
    void init_ValidResources_LoadsPromptsIntoMap() throws IOException {
        // Arrange
        var res1 = mock(Resource.class);
        var res2 = mock(Resource.class);

        when(res1.getFilename()).thenReturn("interviewer.txt");
        when(res1.getContentAsString(StandardCharsets.UTF_8)).thenReturn("Interviewer content");

        when(res2.getFilename()).thenReturn("final.txt");
        when(res2.getContentAsString(StandardCharsets.UTF_8)).thenReturn("Final content");

        when(resourcePatternResolver.getResources(PROMPTS_PATH)).thenReturn(new Resource[]{res1, res2});

        // Act
        promptRegistryService.init();

        // Assert
        assertAll(
                () -> assertEquals("Interviewer content", promptRegistryService.getPrompt("interviewer")),
                () -> assertEquals("Final content", promptRegistryService.getPrompt("final"))
        );
    }

    @Test
    @DisplayName("init: should throw 500 AppException when IOException occurs")
    void init_IoException_ThrowsAppException() throws IOException {
        // Arrange
        when(resourcePatternResolver.getResources(PROMPTS_PATH)).thenThrow(new IOException("Disk error"));

        // Act & Assert
        var exception = assertThrows(AppException.class, () -> promptRegistryService.init());
        assertAll(
                () -> assertEquals(500, exception.getStatusCode()),
                () -> assertTrue(exception.getMessage().contains("Critical failure"))
        );
    }

    @Test
    @DisplayName("init: should skip files that do not end with .txt")
    void init_NonTxtFiles_SkipsFiles() throws IOException {
        // Arrange
        var res1 = mock(Resource.class);
        when(res1.getFilename()).thenReturn("README.md"); // Not a .txt file

        when(resourcePatternResolver.getResources(PROMPTS_PATH)).thenReturn(new Resource[]{res1});

        // Act
        promptRegistryService.init();

        // Assert
        assertThrows(AppException.class, () -> promptRegistryService.getPrompt("README"));
    }

    // --- getPrompt Tests ---

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "invalid_prompt", " "})
    @DisplayName("getPrompt: should throw 500 AppException when prompt name is missing in map")
    void getPrompt_MissingName_ThrowsAppException(String promptName) throws IOException {
        // Arrange
        when(resourcePatternResolver.getResources(PROMPTS_PATH)).thenReturn(new Resource[]{});
        promptRegistryService.init();

        // Act & Assert
        var exception = assertThrows(AppException.class, () -> promptRegistryService.getPrompt(promptName));
        assertAll(
                () -> assertEquals(500, exception.getStatusCode()),
                () -> assertTrue(exception.getMessage().contains("missing"))
        );
    }

    @Test
    @DisplayName("getPrompt: should handle empty content by trimming correctly")
    void getPrompt_EmptyFile_ReturnsTrimmedResult() throws IOException {
        // Arrange
        var res = mock(Resource.class);
        when(res.getFilename()).thenReturn("empty.txt");
        when(res.getContentAsString(StandardCharsets.UTF_8)).thenReturn("   trimmed content   ");

        when(resourcePatternResolver.getResources(PROMPTS_PATH)).thenReturn(new Resource[]{res});
        promptRegistryService.init();

        // Act
        var result = promptRegistryService.getPrompt("empty");

        // Assert
        assertEquals("trimmed content", result);
    }
}