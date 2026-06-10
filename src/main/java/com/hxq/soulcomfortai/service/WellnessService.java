package com.hxq.soulcomfortai.service;

import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.ai.SoulComfortService;
import com.hxq.soulcomfortai.config.PromptTemplateLoader;
import com.hxq.soulcomfortai.config.SoulRedisProperties;
import com.hxq.soulcomfortai.entity.EmotionRecord;
import com.hxq.soulcomfortai.repository.EmotionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class WellnessService {

    private static final List<String> NEGATIVE_EMOTIONS = List.of("sad", "anxious", "lost", "angry");

    private final EmotionRepository emotionRepository;
    private final SoulComfortService soulComfortService;
    private final StringRedisTemplate redis;
    private final SoulRedisProperties soulRedisProperties;
    private final PromptTemplateLoader promptTemplateLoader;

    public WellnessService(EmotionRepository emotionRepository,
                           SoulComfortService soulComfortService,
                           StringRedisTemplate redis,
                           SoulRedisProperties soulRedisProperties,
                           PromptTemplateLoader promptTemplateLoader) {
        this.emotionRepository = emotionRepository;
        this.soulComfortService = soulComfortService;
        this.redis = redis;
        this.soulRedisProperties = soulRedisProperties;
        this.promptTemplateLoader = promptTemplateLoader;
    }

    public Map<String, Object> dailyGreeting(String userId) {
        String today = LocalDate.now().toString();
        String cacheKey = RedisConstants.dailyGreetingKey(userId, today);

        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("greeting", cached);
            data.put("date", today);
            return data;
        }

        String weekStart = LocalDate.now().minusDays(6).toString();
        String weekEnd = today;
        List<EmotionRecord> weekRecords = emotionRepository.findByDateRange(userId, weekStart, weekEnd);

        String emotionTrend = weekRecords.isEmpty() ? "暂无数据"
                : weekRecords.stream().map(EmotionRecord::getEmotion)
                        .distinct().collect(Collectors.joining("、"));
        String lastEmotion = weekRecords.isEmpty() ? "未知"
                : weekRecords.get(weekRecords.size() - 1).getEmotion();
        String currentTime = LocalTime.now().getHour() < 12 ? "上午" : "下午";

        Map<String, String> params = Map.of(
                "emotion_trend", emotionTrend,
                "last_emotion", lastEmotion,
                "user_nickname", "朋友",
                "current_time", currentTime);
        String prompt = promptTemplateLoader.render("daily_greeting", params);

        String greeting = soulComfortService.chatForReport(prompt);
        redis.opsForValue().set(cacheKey, greeting, 24, TimeUnit.HOURS);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("greeting", greeting);
        data.put("date", today);
        return data;
    }

    public Map<String, Object> emotionAlert(String userId) {
        String weekStart = LocalDate.now().minusDays(6).toString();
        String today = LocalDate.now().toString();
        List<EmotionRecord> weekRecords = emotionRepository.findByDateRange(userId, weekStart, today);

        long negativeCount = weekRecords.stream()
                .filter(r -> r.getEmotion() != null && NEGATIVE_EMOTIONS.contains(r.getEmotion().toLowerCase()))
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalRecords", weekRecords.size());
        data.put("negativeCount", negativeCount);
        data.put("negativeRate", weekRecords.isEmpty() ? 0
                : Math.round(negativeCount * 100.0 / weekRecords.size()));
        data.put("needAlert", weekRecords.size() >= 5 && (double) negativeCount / weekRecords.size() > 0.6);
        return data;
    }
}