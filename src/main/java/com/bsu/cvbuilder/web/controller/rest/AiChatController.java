package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.annotation.agreement.AgreementRequire;
import com.bsu.cvbuilder.domain.dto.ai.AiRequestDto;
import com.bsu.cvbuilder.domain.dto.ai.ResumeFlowFormDto;
import com.bsu.cvbuilder.domain.entity.AiChat;
import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ChatService;
import com.bsu.cvbuilder.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat Management", description = "Endpoints for managing AI chat sessions, messaging, and resume data extraction.")
public class AiChatController {

    private final ResumeService resumeDataExtractorService;
    private final AiService aiService;
    private final ChatService chatService;

    @Operation(summary = "Get User's AI Chats", description = "Retrieves a paginated list of AI chat history for the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved chat list"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated", content = @Content)
    })
    @GetMapping
    public Page<AiChat> getAiChats(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(required = false, name = "page", defaultValue = "0") Integer page,

            @Parameter(description = "Number of items per page", example = "5")
            @RequestParam(required = false, name = "size", defaultValue = "5") Integer size,

            @Parameter(description = "Field to sort by", example = "createdAt")
            @RequestParam(required = false, name = "sort", defaultValue = "createdAt") String sort,

            @Parameter(description = "Sort direction (asc or desc)", example = "asc")
            @RequestParam(required = false, name = "direction", defaultValue = "asc") String direction
    ) {
        Sort sorting = Sort.by(Sort.Direction.fromString(direction), sort);
        return chatService.findAllByCurrentUser(PageRequest.of(page, size, sorting));
    }

    @Operation(summary = "Get Specific Chat", description = "Retrieves detailed information for a specific chat session by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved chat details"),
            @ApiResponse(responseCode = "404", description = "Chat not found or access denied", content = @Content)
    })
    @GetMapping("/chat/{chatId}")
    public AiChat findAll(
            @Parameter(description = "UUID of the chat to retrieve", required = true)
            @PathVariable UUID chatId
    ) {
        return chatService.getChatById(chatId);
    }

    @Operation(summary = "Create New Chat", description = "Initializes a new AI chat session. Requires user agreement.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chat session created successfully"),
            @ApiResponse(responseCode = "403", description = "User agreement missing", content = @Content)
    })
    @AgreementRequire
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public AiChat create() {
        return chatService.createAiChat(UUID.randomUUID());
    }

    @Operation(summary = "Send Message to AI", description = "Sends a prompt to the AI service and returns the text response. Requires user agreement.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "403", description = "User agreement missing", content = @Content)
    })
    @AgreementRequire
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String ask(
            @Parameter(description = "DTO containing the user prompt and context", required = true)
            @RequestBody AiRequestDto aiRequestDto
    ) {
        return aiService.call(aiRequestDto);
    }

    @Operation(summary = "Extract Resume Data", description = "Analyzes a chat session and extracts structured resume data. Requires user agreement.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resume extracted successfully", content = @Content(schema = @Schema(implementation = Resume.class))),
            @ApiResponse(responseCode = "404", description = "Chat ID not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "User agreement missing", content = @Content)
    })
    @AgreementRequire
    @GetMapping("/resume/{chatId}")
    @ResponseStatus(HttpStatus.OK)
    public Resume extract(
            @Parameter(description = "UUID string of the chat source", required = true)
            @PathVariable String chatId
    ) {
        return resumeDataExtractorService.findByChatId(UUID.fromString(chatId));
    }

    @PostMapping("/flow")
    public Resume process(@Valid @RequestBody ResumeFlowFormDto  resumeFlowFormDto) {
        return resumeFlowService.generate(resumeFlowFormDto);
    }
}