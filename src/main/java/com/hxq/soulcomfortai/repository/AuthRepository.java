package com.hxq.soulcomfortai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
public class AuthRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public AuthRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public String nextUserId() {
        Long id = redis.opsForValue().increment(RedisConstants.SEQ_USER_ID);
        return RedisConstants.USER_ID_PREFIX + id;
    }

    public void save(User user) {
        try {
            String json = objectMapper.writeValueAsString(user);
            redis.opsForValue().set(RedisConstants.userKey(user.getId()), json);
            redis.opsForValue().set(RedisConstants.usernameIndexKey(user.getUsername()), user.getId());
        } catch (JsonProcessingException e) {
            log.error("保存用户失败 userId={}", user.getId(), e);
            throw new RuntimeException("保存用户失败", e);
        }
    }

    public Optional<User> findById(String userId) {
        try {
            String json = redis.opsForValue().get(RedisConstants.userKey(userId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, User.class));
        } catch (JsonProcessingException e) {
            log.error("查询用户失败 userId={}", userId, e);
            return Optional.empty();
        }
    }

    public Optional<User> findByUsername(String username) {
        String userId = redis.opsForValue().get(RedisConstants.usernameIndexKey(username));
        if (userId == null) {
            return Optional.empty();
        }
        return findById(userId);
    }

    public boolean existsByUsername(String username) {
        return Boolean.TRUE.equals(redis.hasKey(RedisConstants.usernameIndexKey(username)));
    }
}