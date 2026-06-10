package com.hxq.soulcomfortai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.config.SoulRedisProperties;
import com.hxq.soulcomfortai.dto.response.PageResult;
import com.hxq.soulcomfortai.entity.ChatMessageVO;
import com.hxq.soulcomfortai.entity.Conversation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class ConversationRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SoulRedisProperties soulRedisProperties;

    public ConversationRepository(StringRedisTemplate redis,
                                  ObjectMapper objectMapper,
                                  SoulRedisProperties soulRedisProperties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.soulRedisProperties = soulRedisProperties;
    }

    public String nextId() {
        Long id = redis.opsForValue().increment(RedisConstants.SEQ_CONV_ID);
        return RedisConstants.CONV_ID_PREFIX + id;
    }

    public void save(Conversation conv) {
        try {
            String key = RedisConstants.convKey(conv.getId());
            String json = objectMapper.writeValueAsString(conv);
            long ttl = soulRedisProperties.getTtlSeconds();
            redis.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);

            String indexKey = RedisConstants.userConvsKey(conv.getUserId());
            redis.opsForZSet().add(indexKey, conv.getId(), conv.getUpdatedAt());
            redis.expire(indexKey, ttl, TimeUnit.SECONDS);

            if (conv.getTag() != null) {
                String tagKey = RedisConstants.userConvsByTagKey(conv.getUserId(), conv.getTag());
                redis.opsForZSet().add(tagKey, conv.getId(), conv.getUpdatedAt());
                redis.expire(tagKey, ttl, TimeUnit.SECONDS);
            }
        } catch (JsonProcessingException e) {
            log.error("保存对话失败 convId={}", conv.getId(), e);
            throw new RuntimeException("保存对话失败", e);
        }
    }

    public Optional<Conversation> findById(String convId) {
        try {
            String json = redis.opsForValue().get(RedisConstants.convKey(convId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, Conversation.class));
        } catch (JsonProcessingException e) {
            log.error("查询对话失败 convId={}", convId, e);
            return Optional.empty();
        }
    }

    public void appendMessage(String convId, String userId, ChatMessageVO message) {
        try {
            String msgKey = RedisConstants.convMessagesKey(convId);
            String json = objectMapper.writeValueAsString(message);
            long ttl = soulRedisProperties.getTtlSeconds();

            redis.opsForList().rightPush(msgKey, json);

            Long count = redis.opsForList().size(msgKey);
            int maxMessages = soulRedisProperties.getMaxMessages();
            if (count != null && count > maxMessages) {
                redis.opsForList().trim(msgKey, count - maxMessages, -1);
            }

            redis.expire(RedisConstants.convKey(convId), ttl, TimeUnit.SECONDS);
            redis.expire(msgKey, ttl, TimeUnit.SECONDS);
            redis.expire(RedisConstants.userConvsKey(userId), ttl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("追加消息失败 convId={}", convId, e);
            throw new RuntimeException("追加消息失败", e);
        }
    }

    public void updateConversationTimestamp(String convId) {
        findById(convId).ifPresent(conv -> {
            conv.setUpdatedAt(System.currentTimeMillis());
            try {
                String key = RedisConstants.convKey(convId);
                String json = objectMapper.writeValueAsString(conv);
                long ttl = soulRedisProperties.getTtlSeconds();
                redis.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);

                String indexKey = RedisConstants.userConvsKey(conv.getUserId());
                redis.opsForZSet().add(indexKey, convId, conv.getUpdatedAt());
            } catch (JsonProcessingException e) {
                log.error("更新对话时间戳失败 convId={}", convId, e);
            }
        });
    }

    public List<ChatMessageVO> findMessages(String convId) {
        return findMessages(convId, 0, -1);
    }

    public List<ChatMessageVO> findMessages(String convId, int offset, int limit) {
        String msgKey = RedisConstants.convMessagesKey(convId);
        int end = limit <= 0 ? -1 : offset + limit - 1;
        List<String> jsonList = redis.opsForList().range(msgKey, offset, end);
        if (jsonList == null || jsonList.isEmpty()) {
            return List.of();
        }
        List<ChatMessageVO> messages = new ArrayList<>();
        for (String json : jsonList) {
            try {
                messages.add(objectMapper.readValue(json, ChatMessageVO.class));
            } catch (JsonProcessingException e) {
                log.error("解析消息失败 convId={}", convId, e);
            }
        }
        return messages;
    }

    public long countMessages(String convId) {
        String msgKey = RedisConstants.convMessagesKey(convId);
        Long count = redis.opsForList().size(msgKey);
        return count == null ? 0 : count;
    }

    public void updateTitle(String convId, String title) {
        String key = RedisConstants.convKey(convId);
        String json = redis.opsForValue().get(key);
        if (json == null) {
            throw new RuntimeException("对话不存在或已过期");
        }
        try {
            Conversation conv = objectMapper.readValue(json, Conversation.class);
            conv.setTitle(title);
            conv.setUpdatedAt(System.currentTimeMillis());
            String newJson = objectMapper.writeValueAsString(conv);
            long ttl = soulRedisProperties.getTtlSeconds();
            redis.opsForValue().set(key, newJson, ttl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("重命名对话失败 convId={}", convId, e);
            throw new RuntimeException("重命名对话失败", e);
        }
    }

    public PageResult<Conversation> findByUserId(String userId, int page, int size) {
        String indexKey = RedisConstants.userConvsKey(userId);
        Long total = redis.opsForZSet().zCard(indexKey);
        if (total == null || total == 0) {
            return new PageResult<>(List.of(), 0, page, size);
        }

        int start = (page - 1) * size;
        int end = start + size - 1;
        Set<String> convIds = redis.opsForZSet().reverseRange(indexKey, start, end);

        List<Conversation> conversations = new ArrayList<>();
        if (convIds != null) {
            for (String id : convIds) {
                Optional<Conversation> opt = findById(id);
                if (opt.isPresent()) {
                    conversations.add(opt.get());
                } else {
                    redis.opsForZSet().remove(indexKey, id);
                }
            }
        }

        return new PageResult<>(conversations, total, page, size);
    }

    public void delete(String convId, String userId) {
        Optional<Conversation> opt = findById(convId);
        opt.ifPresent(conv -> {
            if (conv.getTag() != null) {
                redis.opsForZSet().remove(RedisConstants.userConvsByTagKey(userId, conv.getTag()), convId);
            }
        });
        redis.delete(RedisConstants.convKey(convId));
        redis.delete(RedisConstants.convMessagesKey(convId));
        redis.opsForZSet().remove(RedisConstants.userConvsKey(userId), convId);
    }

    public void updateTag(String convId, String userId, String tag) {
        Optional<Conversation> opt = findById(convId);
        if (opt.isEmpty()) {
            throw new RuntimeException("对话不存在或已过期");
        }
        Conversation conv = opt.get();
        String oldTag = conv.getTag();
        conv.setTag(tag);
        conv.setUpdatedAt(System.currentTimeMillis());
        try {
            String key = RedisConstants.convKey(convId);
            String json = objectMapper.writeValueAsString(conv);
            long ttl = soulRedisProperties.getTtlSeconds();
            redis.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);

            if (oldTag != null) {
                redis.opsForZSet().remove(RedisConstants.userConvsByTagKey(userId, oldTag), convId);
            }
            if (tag != null) {
                String tagKey = RedisConstants.userConvsByTagKey(userId, tag);
                redis.opsForZSet().add(tagKey, convId, conv.getUpdatedAt());
                redis.expire(tagKey, ttl, TimeUnit.SECONDS);
            }
        } catch (JsonProcessingException e) {
            log.error("更新对话标签失败 convId={}", convId, e);
            throw new RuntimeException("更新对话标签失败", e);
        }
    }

    public PageResult<Conversation> findByUserIdAndTag(String userId, String tag, int page, int size) {
        String tagKey = RedisConstants.userConvsByTagKey(userId, tag);
        Long total = redis.opsForZSet().zCard(tagKey);
        if (total == null || total == 0) {
            return new PageResult<>(List.of(), 0, page, size);
        }

        int start = (page - 1) * size;
        int end = start + size - 1;
        Set<String> convIds = redis.opsForZSet().reverseRange(tagKey, start, end);

        List<Conversation> conversations = new ArrayList<>();
        if (convIds != null) {
            for (String id : convIds) {
                Optional<Conversation> convOpt = findById(id);
                if (convOpt.isPresent()) {
                    conversations.add(convOpt.get());
                } else {
                    redis.opsForZSet().remove(tagKey, id);
                }
            }
        }

        return new PageResult<>(conversations, total, page, size);
    }

    public List<String> getUserTags(String userId) {
        Set<String> tagKeys = redis.keys(RedisConstants.userConvsByTagKey(userId, "*"));
        if (tagKeys == null || tagKeys.isEmpty()) {
            return List.of();
        }
        String prefix = RedisConstants.userConvsByTagKey(userId, "");
        return tagKeys.stream()
                .map(k -> k.substring(prefix.length()))
                .toList();
    }

    private static final String SAVE_MESSAGES_SCRIPT =
            "redis.call('DEL', KEYS[1]) " +
            "for i = 1, #ARGV do " +
            "  redis.call('RPUSH', KEYS[1], ARGV[i]) " +
            "end " +
            "redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "return 1";

    public void saveMessages(String convId, String userId, List<ChatMessageVO> messages) {
        try {
            String msgKey = RedisConstants.convMessagesKey(convId);
            long ttl = soulRedisProperties.getTtlSeconds();

            List<String> jsonList = new ArrayList<>();
            for (ChatMessageVO msg : messages) {
                jsonList.add(objectMapper.writeValueAsString(msg));
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(SAVE_MESSAGES_SCRIPT, Long.class);
            redis.execute(script, List.of(msgKey), jsonList.toArray());

            redis.expire(RedisConstants.convKey(convId), ttl, TimeUnit.SECONDS);
            redis.expire(RedisConstants.userConvsKey(userId), ttl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("保存消息列表失败 convId={}", convId, e);
            throw new RuntimeException("保存消息列表失败", e);
        }
    }
}