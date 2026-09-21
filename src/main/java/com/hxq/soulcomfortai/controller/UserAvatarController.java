package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.dto.ApiResponse;
import com.hxq.soulcomfortai.service.AvatarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/user/avatar")
public class UserAvatarController {

    private final AvatarService avatarService;

    public UserAvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    /** 上传用户头像：文件存阿里云 OSS，地址写入 Redis，返回可访问地址 */
    @PostMapping
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestAttribute("userId") String userId) {
        String url = avatarService.uploadAvatar(userId, file);
        return ApiResponse.success(Map.of("avatarUrl", url));
    }

    /** 查询用户头像地址（Redis 中缓存的 OSS URL） */
    @GetMapping
    public ApiResponse<Map<String, String>> getCurrent(@RequestAttribute("userId") String userId) {
        String url = avatarService.getAvatarUrl(userId);
        return ApiResponse.success(Map.of("avatarUrl", url == null ? "" : url));
    }
}