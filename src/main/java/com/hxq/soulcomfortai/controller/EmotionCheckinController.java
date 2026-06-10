package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.dto.ApiResponse;
import com.hxq.soulcomfortai.entity.EmotionCheckin;
import com.hxq.soulcomfortai.repository.EmotionCheckinRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/checkin")
public class EmotionCheckinController {

    private static final Map<String, String> EMOTION_LABELS = Map.of(
            "happy", "😊 开心", "calm", "😌 平静", "sad", "😢 难过",
            "anxious", "😰 焦虑", "angry", "😡 生气", "energetic", "💪 充满活力",
            "tired", "😴 疲惫", "grateful", "🙏 感恩"
    );

    private final EmotionCheckinRepository checkinRepository;

    public EmotionCheckinController(EmotionCheckinRepository checkinRepository) {
        this.checkinRepository = checkinRepository;
    }

    @PostMapping
    public ApiResponse<EmotionCheckin> checkin(@RequestAttribute("userId") String userId,
                                                @RequestBody Map<String, String> body) {
        String emotion = body.get("emotion");
        String note = body.getOrDefault("note", "");

        String today = LocalDate.now().toString();
        EmotionCheckin existing = checkinRepository.findByDate(userId, today);
        if (existing != null) {
            return ApiResponse.fail(400, "今天已经签到过了");
        }

        EmotionCheckin checkin = EmotionCheckin.builder()
                .id(checkinRepository.nextId())
                .userId(userId)
                .emotion(emotion)
                .note(note)
                .date(today)
                .timestamp(System.currentTimeMillis())
                .build();
        checkinRepository.save(checkin);
        return ApiResponse.success(checkin);
    }

    @GetMapping("/today")
    public ApiResponse<Map<String, Object>> getToday(@RequestAttribute("userId") String userId) {
        String today = LocalDate.now().toString();
        EmotionCheckin checkin = checkinRepository.findByDate(userId, today);

        Map<String, Object> result = new HashMap<>();
        result.put("checked", checkin != null);
        if (checkin != null) {
            result.put("emotion", checkin.getEmotion());
            result.put("note", checkin.getNote());
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        if (size > 50) {
            size = 50;
        }

        Map<String, Object> pageResult = checkinRepository.findByUserWithPagination(userId, page, size, keyword);

        @SuppressWarnings("unchecked")
        List<EmotionCheckin> records = (List<EmotionCheckin>) pageResult.get("records");
        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (EmotionCheckin checkin : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", checkin.getId());
            item.put("emotion", checkin.getEmotion());
            item.put("emotionLabel", EMOTION_LABELS.getOrDefault(checkin.getEmotion(), checkin.getEmotion()));
            item.put("note", checkin.getNote());
            item.put("date", checkin.getDate());
            item.put("timestamp", checkin.getTimestamp());
            enrichedRecords.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", pageResult.get("total"));
        result.put("page", page);
        result.put("size", size);
        result.put("records", enrichedRecords);
        return ApiResponse.success(result);
    }

    @GetMapping("/calendar")
    public ApiResponse<Map<String, Object>> calendar(
            @RequestAttribute("userId") String userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth ym;
        if (year != null && month != null) {
            ym = YearMonth.of(year, month);
        } else {
            ym = YearMonth.now();
        }

        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        List<EmotionCheckin> records = checkinRepository.findByDateRange(
                userId, firstDay.toString(), lastDay.toString());

        List<Map<String, Object>> checkedDays = new ArrayList<>();
        for (EmotionCheckin checkin : records) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", checkin.getDate());
            day.put("emotion", checkin.getEmotion());
            day.put("emotionLabel", EMOTION_LABELS.getOrDefault(checkin.getEmotion(), checkin.getEmotion()));
            day.put("note", checkin.getNote());
            checkedDays.add(day);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", ym.getYear());
        result.put("month", ym.getMonthValue());
        result.put("checkedDays", checkedDays);
        return ApiResponse.success(result);
    }
}