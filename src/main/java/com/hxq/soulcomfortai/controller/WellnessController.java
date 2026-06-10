package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.dto.ApiResponse;
import com.hxq.soulcomfortai.service.WellnessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/wellness")
public class WellnessController {

    private final WellnessService wellnessService;

    public WellnessController(WellnessService wellnessService) {
        this.wellnessService = wellnessService;
    }

    @GetMapping("/daily-greeting")
    public ApiResponse<Map<String, Object>> dailyGreeting(
            @RequestAttribute("userId") String userId) {
        return ApiResponse.success(wellnessService.dailyGreeting(userId));
    }

    @GetMapping("/emotion-alert")
    public ApiResponse<Map<String, Object>> emotionAlert(
            @RequestAttribute("userId") String userId) {
        return ApiResponse.success(wellnessService.emotionAlert(userId));
    }
}