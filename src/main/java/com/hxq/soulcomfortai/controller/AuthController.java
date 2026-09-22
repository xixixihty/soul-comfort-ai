package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.dto.ApiResponse;
import com.hxq.soulcomfortai.dto.request.LoginRequest;
import com.hxq.soulcomfortai.dto.request.RegisterRequest;
import com.hxq.soulcomfortai.dto.request.UpdateProfileRequest;
import com.hxq.soulcomfortai.dto.response.LoginResponse;
import com.hxq.soulcomfortai.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(
                request.getUsername(), request.getPassword(), request.getNickname());
        return ApiResponse.success(response);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getUsername(), request.getPassword());
        return ApiResponse.success(response);
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse.UserInfo> getCurrentUser(
            @RequestAttribute("userId") String userId) {
        return ApiResponse.success(authService.getCurrentUser(userId));
    }

    @PutMapping("/profile")
    public ApiResponse<LoginResponse.UserInfo> updateProfile(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(authService.updateProfile(userId, request.getNickname()));
    }
}