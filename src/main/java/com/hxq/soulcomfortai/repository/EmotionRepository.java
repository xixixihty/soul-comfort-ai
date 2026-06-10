package com.hxq.soulcomfortai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.config.SoulRedisProperties;
import com.hxq.soulcomfortai.entity.EmotionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class EmotionRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SoulRedisProperties soulRedisProperties;

    public EmotionRepository(StringRedisTemplate redis,
                             ObjectMapper objectMapper,
                             SoulRedisProperties soulRedisProperties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.soulRedisProperties = soulRedisProperties;
    }

    public String nextId() {
        Long id = redis.opsForValue().increment(RedisConstants.SEQ_EMOTION_ID);
        return RedisConstants.EMOTION_ID_PREFIX + id;
    }

    private static final String SAVE_EMOTION_SCRIPT =
            "redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[5]) " +
            "redis.call('ZADD', KEYS[2], ARGV[2], ARGV[3]) " +
            "redis.call('EXPIRE', KEYS[2], ARGV[5]) " +
            "redis.call('HINCRBY', KEYS[3], 'total', 1) " +
            "redis.call('HINCRBY', KEYS[3], ARGV[4], 1) " +
            "redis.call('EXPIRE', KEYS[3], ARGV[5]) " +
            "return 1";

    public void save(EmotionRecord record) {
        try {
            String json = objectMapper.writeValueAsString(record);
            long ttl = soulRedisProperties.getTtlSeconds();

            String emotionKey = RedisConstants.emotionKey(record.getId());
            String dailyKey = RedisConstants.userEmotionDailyKey(record.getUserId(), record.getDate());
            String summaryKey = RedisConstants.userEmotionSummaryKey(record.getUserId());

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(SAVE_EMOTION_SCRIPT, Long.class);
            redis.execute(script,
                    List.of(emotionKey, dailyKey, summaryKey),
                    json,
                    String.valueOf(record.getTimestamp()),
                    record.getId(),
                    record.getEmotion(),
                    String.valueOf(ttl));
        } catch (JsonProcessingException e) {
            log.error("保存情绪记录失败 emotionId={}", record.getId(), e);
            throw new RuntimeException("保存情绪记录失败", e);
        }
    }

    public List<EmotionRecord> findByDateRange(String userId, String startDate, String endDate) {
        List<EmotionRecord> records = new ArrayList<>();
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        java.time.LocalDate end = java.time.LocalDate.parse(endDate);

        for (java.time.LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.toString();
            Set<String> ids = redis.opsForZSet()
                    .range(RedisConstants.userEmotionDailyKey(userId, dateStr), 0, -1);
            if (ids != null) {
                for (String id : ids) {
                    String json = redis.opsForValue().get(RedisConstants.emotionKey(id));
                    if (json != null) {
                        try {
                            records.add(objectMapper.readValue(json, EmotionRecord.class));
                        } catch (JsonProcessingException e) {
                            log.error("解析情绪记录失败 emotionId={}", id, e);
                        }
                    }
                }
            }
        }
        records.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        return records;
    }

    public List<EmotionRecord> findByDate(String userId, String date) {
        Set<String> ids = redis.opsForZSet()
                .range(RedisConstants.userEmotionDailyKey(userId, date), 0, -1);
        List<EmotionRecord> records = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                String json = redis.opsForValue().get(RedisConstants.emotionKey(id));
                if (json != null) {
                    try {
                        records.add(objectMapper.readValue(json, EmotionRecord.class));
                    } catch (JsonProcessingException e) {
                        log.error("解析情绪记录失败 emotionId={}", id, e);
                    }
                }
            }
        }
        return records;
    }
}