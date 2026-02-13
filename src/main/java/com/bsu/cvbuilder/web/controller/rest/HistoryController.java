package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.dto.history.HistoryEventsDto;
import com.bsu.cvbuilder.domain.entity.History;
import com.bsu.cvbuilder.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Tag(name = "History Management", description = "Endpoints for retrieving user activity history and operation logs.")
public class HistoryController {

    private final HistoryService historyService;

    @Operation(
            summary = "Get User History",
            description = "Retrieves the complete operation history for a specific user by their user ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user history",
                    content = @Content(schema = @Schema(implementation = History.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found or history does not exist"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid user ID format"
            )
    })
    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public History findAllHistoryByUser(
            @Parameter(
                    description = "UUID of the user whose history is to be retrieved",
                    required = true,
                    example = "123e4567-e89b-12d3-a456-426614174000"
            )
            @PathVariable String userId,

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
        return historyService.findByUserId(userId, PageRequest.of(page, size, sorting));
    }

    @GetMapping
    public HistoryEventsDto getEvents(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0", required = false) int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10", required = false) int size
    ) {
        return historyService.findByCurrentUser(page, size);
    }


}