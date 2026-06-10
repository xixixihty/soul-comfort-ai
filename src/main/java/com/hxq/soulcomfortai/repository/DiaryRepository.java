package com.hxq.soulcomfortai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.config.SoulRedisProperties;
import com.hxq.soulcomfortai.dto.response.PageResult;
import com.hxq.soulcomfortai.entity.Diary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class DiaryRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SoulRedisProperties soulRedisProperties;

    public DiaryRepository(StringRedisTemplate redis,
                           ObjectMapper objectMapper,
                           SoulRedisProperties soulRedisProperties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.soulRedisProperties = soulRedisProperties;
    }

    public String nextId() {
        Long id = redis.opsForValue().increment(RedisConstants.SEQ_DIARY_ID);
        return RedisConstants.DIARY_ID_PREFIX + id;
    }

    public void save(Diary diary) {
        try {
            String key = RedisConstants.diaryKey(diary.getId());
            String json = objectMapper.writeValueAsString(diary);
            long ttl = soulRedisProperties.getTtlSeconds();
            redis.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);

            String indexKey = RedisConstants.userDiariesKey(diary.getUserId());
            redis.opsForZSet().add(indexKey, diary.getId(), diary.getUpdatedAt());
            redis.expire(indexKey, ttl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("保存日记失败 diaryId={}", diary.getId(), e);
            throw new RuntimeException("保存日记失败", e);
        }
    }

    public Optional<Diary> findById(String diaryId) {
        try {
            String json = redis.opsForValue().get(RedisConstants.diaryKey(diaryId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, Diary.class));
        } catch (JsonProcessingException e) {
            log.error("查询日记失败 diaryId={}", diaryId, e);
            return Optional.empty();
        }
    }

    public void update(Diary diary) {
        try {
            String key = RedisConstants.diaryKey(diary.getId());
            String json = objectMapper.writeValueAsString(diary);
            long ttl = soulRedisProperties.getTtlSeconds();
            redis.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);

            String indexKey = RedisConstants.userDiariesKey(diary.getUserId());
            redis.opsForZSet().add(indexKey, diary.getId(), diary.getUpdatedAt());
        } catch (JsonProcessingException e) {
            log.error("更新日记失败 diaryId={}", diary.getId(), e);
            throw new RuntimeException("更新日记失败", e);
        }
    }

    public PageResult<Diary> findByUserId(String userId, int page, int size) {
        String indexKey = RedisConstants.userDiariesKey(userId);
        Long total = redis.opsForZSet().zCard(indexKey);
        if (total == null || total == 0) {
            return new PageResult<>(List.of(), 0, page, size);
        }

        int start = (page - 1) * size;
        int end = start + size - 1;
        Set<String> diaryIds = redis.opsForZSet().reverseRange(indexKey, start, end);

        List<Diary> diaries = new ArrayList<>();
        if (diaryIds != null) {
            for (String id : diaryIds) {
                Optional<Diary> opt = findById(id);
                if (opt.isPresent()) {
                    diaries.add(opt.get());
                } else {
                    redis.opsForZSet().remove(indexKey, id);
                }
            }
        }

        return new PageResult<>(diaries, total, page, size);
    }

    public void delete(String diaryId, String userId) {
        redis.delete(RedisConstants.diaryKey(diaryId));
        redis.opsForZSet().remove(RedisConstants.userDiariesKey(userId), diaryId);
    }
}