package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.dto.ApiResponse;
import com.hxq.soulcomfortai.service.care.ProactiveCareService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 主动关怀（情绪灯塔）接口。/care/** 已被登录拦截器覆盖（WebMvcConfig addPathPatterns("/**")），
 * userId 一律取自令牌注入的 @RequestAttribute，不接受入参伪造。
 */
@RestController
@RequestMapping("/care")
public class CareController {

    private final ProactiveCareService careService;

    public CareController(ProactiveCareService careService) {
        this.careService = careService;
    }

    /** 页面加载时轮询：趋势（晴雨云头像）+ 待展示的关怀卡（含按需生成） */
    @GetMapping("/pending")
    public ApiResponse<Map<String, Object>> pending(@RequestAttribute("userId") String userId) {
        return ApiResponse.success(careService.pendingOrGenerate(userId));
    }

    /** 用户对关怀卡的回执：opened（点击进入）/ dismissed（主动关闭，用于降级频控） */
    @PostMapping("/{careId}/ack")
    public ApiResponse<Void> ack(@RequestAttribute("userId") String userId,
                                 @PathVariable String careId,
                                 @RequestBody Map<String, String> body) {
        careService.ack(userId, careId, body.getOrDefault("action", "opened"));
        return ApiResponse.success(null);
    }

    @GetMapping("/settings")
    public ApiResponse<Map<String, Object>> settings(@RequestAttribute("userId") String userId) {
        return ApiResponse.success(careService.settings(userId));
    }

    @PutMapping("/switch")
    public ApiResponse<Void> switchCare(@RequestAttribute("userId") String userId,
                                        @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ApiResponse.fail(400, "缺少 enabled 参数");
        }
        careService.setEnabled(userId, enabled);
        return ApiResponse.success(null);
    }

    /** 会话内"甜弈先开口"：点关怀卡 / 点解忧纸星（带 refDate）都走这里 */
    @PostMapping("/opener")
    public ApiResponse<Map<String, Object>> opener(@RequestAttribute("userId") String userId,
                                                   @RequestBody(required = false) Map<String, String> body) {
        Map<String, String> safeBody = body == null ? Map.of() : body;
        return ApiResponse.success(careService.opener(userId, safeBody.get("convId"), safeBody.get("refDate")));
    }

    /** 情绪年轮 / 解忧纸星数据源：某年全部打卡的日期+心情词（无 note） */
    @GetMapping("/mood-calendar")
    public ApiResponse<List<Map<String, Object>>> moodCalendar(@RequestAttribute("userId") String userId,
                                                               @RequestParam(required = false) Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        if (y < 2000 || y > 2100) {
            return ApiResponse.fail(400, "年份不合法");
        }
        return ApiResponse.success(careService.moodCalendar(userId, y));
    }
}
