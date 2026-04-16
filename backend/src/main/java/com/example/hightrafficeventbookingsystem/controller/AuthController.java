package com.example.hightrafficeventbookingsystem.controller;

import com.example.hightrafficeventbookingsystem.dto.AuthRequest;
import com.example.hightrafficeventbookingsystem.dto.AuthResponse;
import com.example.hightrafficeventbookingsystem.dto.RefreshTokenRequest;
import com.example.hightrafficeventbookingsystem.dto.RegisterRequest;
import com.example.hightrafficeventbookingsystem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "Endpoints for registration and login")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new account and returns a JWT token")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Login", description = "Authenticates user credentials and returns a JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh JWT", description = "Returns a new access token and refresh token pair")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
