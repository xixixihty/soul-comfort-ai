package com.hxq.soulcomfortai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.config.SoulRedisProperties;
import com.hxq.soulcomfortai.entity.EmotionCheckin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class EmotionCheckinRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SoulRedisProperties soulRedisProperties;

    public EmotionCheckinRepository(StringRedisTemplate redis,
                                    ObjectMapper objectMapper,
                                    SoulRedisProperties soulRedisProperties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.soulRedisProperties = soulRedisProperties;
    }

    public String nextId() {
        Long id = redis.opsForValue().increment(RedisConstants.SEQ_CHECKIN_ID);
        return RedisConstants.CHECKIN_ID_PREFIX + id;
    }

    public void save(EmotionCheckin checkin) {
        try {
            String json = objectMapper.writeValueAsString(checkin);
            long ttl = soulRedisProperties.getTtlSeconds();

            redis.opsForValue().set(RedisConstants.checkinKey(checkin.getId()), json, ttl, TimeUnit.SECONDS);
            redis.opsForZSet().add(RedisConstants.userCheckinDailyKey(checkin.getUserId(), checkin.getDate()),
                    checkin.getId(), checkin.getTimestamp());
            redis.opsForZSet().add(RedisConstants.userCheckinListKey(checkin.getUserId()),
                    checkin.getId(), checkin.getTimestamp());
            redis.expire(RedisConstants.userCheckinDailyKey(checkin.getUserId(), checkin.getDate()), ttl, TimeUnit.SECONDS);
            redis.expire(RedisConstants.userCheckinListKey(checkin.getUserId()), ttl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("保存签到记录失败 checkinId={}", checkin.getId(), e);
            throw new RuntimeException("保存签到记录失败", e);
        }
    }

    public EmotionCheckin findByDate(String userId, String date) {
        Set<String> ids = redis.opsForZSet()
                .range(RedisConstants.userCheckinDailyKey(userId, date), 0, 0);
        if (ids != null && !ids.isEmpty()) {
            String json = redis.opsForValue().get(RedisConstants.checkinKey(ids.iterator().next()));
            if (json != null) {
                try {
                    return objectMapper.readValue(json, EmotionCheckin.class);
                } catch (JsonProcessingException e) {
                    log.error("解析签到记录失败", e);
                }
            }
        }
        return null;
    }

    public List<EmotionCheckin> findByDateRange(String userId, String startDate, String endDate) {
        List<EmotionCheckin> records = new ArrayList<>();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.toString();
            Set<String> ids = redis.opsForZSet()
                    .range(RedisConstants.userCheckinDailyKey(userId, dateStr), 0, -1);
            if (ids != null) {
                for (String id : ids) {
                    String json = redis.opsForValue().get(RedisConstants.checkinKey(id));
                    if (json != null) {
                        try {
                            records.add(objectMapper.readValue(json, EmotionCheckin.class));
                        } catch (JsonProcessingException e) {
                            log.error("解析签到记录失败 checkinId={}", id, e);
                        }
                    }
                }
            }
        }
        records.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        return records;
    }

    public Map<String, Object> findByUserWithPagination(String userId, int page, int size, String keyword) {
        String listKey = RedisConstants.userCheckinListKey(userId);
        Long total = redis.opsForZSet().zCard(listKey);
        if (total == null || total == 0) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", 0L);
            result.put("records", Collections.emptyList());
            return result;
        }

        int start = (page - 1) * size;
        int end = start + size - 1;
        Set<String> ids = redis.opsForZSet()
                .reverseRange(listKey, start, end);
        if (ids == null || ids.isEmpty()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", total);
            result.put("records", Collections.emptyList());
            return result;
        }

        List<EmotionCheckin> records = new ArrayList<>();
        for (String id : ids) {
            String json = redis.opsForValue().get(RedisConstants.checkinKey(id));
            if (json != null) {
                try {
                    EmotionCheckin checkin = objectMapper.readValue(json, EmotionCheckin.class);
                    if (keyword != null && !keyword.isBlank()) {
                        String note = checkin.getNote();
                        if (note == null || !note.contains(keyword)) {
                            continue;
                        }
                    }
                    records.add(checkin);
                } catch (JsonProcessingException e) {
                    log.error("解析签到记录失败 checkinId={}", id, e);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", keyword != null && !keyword.isBlank() ? (long) records.size() : total);
        result.put("records", records);
        return result;
    }
}