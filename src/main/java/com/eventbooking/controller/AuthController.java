package com.eventbooking.controller;

import com.eventbooking.dto.request.AuthRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.service.impl.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register as ORGANIZER or CUSTOMER.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse.AuthToken> register(@Valid @RequestBody AuthRequest.Register request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * POST /api/auth/login
     * Login and receive a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse.AuthToken> login(@Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
