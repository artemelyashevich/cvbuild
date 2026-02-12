package com.bsu.cvbuilder.web.controller.rest;

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
            @PathVariable String userId
    ) {
        return historyService.findByUserId(userId);
    }
}