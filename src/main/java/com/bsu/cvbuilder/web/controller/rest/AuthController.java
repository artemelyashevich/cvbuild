package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.domain.dto.auth.*;
import com.bsu.cvbuilder.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Authentication management APIs")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with the provided registration details"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data or email already exists"),
            @ApiResponse(responseCode = "422", description = "Validation failed for registration data"),
            @ApiResponse(responseCode = "500", description = "Internal server error during registration")
    })
    public AuthResponse register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User registration details", required = true)
            @RequestBody @Valid RegisterAuthDto authRequest
    ) {
        return authService.register(authRequest);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with email and password, returning JWT tokens"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error during authentication")
    })
    public AuthResponse login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Login credentials", required = true)
            @RequestBody @Valid AuthRequest authRequest
    ) {
        return authService.authenticate(authRequest);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Refresh access token",
            description = "Obtains a new access token using a valid refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tokens successfully refreshed"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token format"),
            @ApiResponse(responseCode = "401", description = "Refresh token expired or revoked"),
            @ApiResponse(responseCode = "500", description = "Internal server error during token refresh")
    })
    public AuthResponse refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh token", required = true)
            @RequestBody @Valid RefreshRequest request
    ) {
        return authService.refreshToken(request);
    }

    @PostMapping("/2fa")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthResponse verify2fa(@Valid @RequestBody Verify2faRequest verify2faRequest) {
        return authService.verify2fa(verify2faRequest);
    }

    @PostMapping("/2fa/refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void verify2faRefresh(@Valid @RequestBody Verify2faRefreshRequest verify2faRefreshRequest) {
        authService.verify2faRefresh(verify2faRefreshRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Logout user",
            description = "Invalidates the current user session and refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully logged out"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error during logout")
    })
    public void logout() {
        authService.logout();
    }
}