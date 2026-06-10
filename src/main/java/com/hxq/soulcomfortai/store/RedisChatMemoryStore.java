package com.hxq.soulcomfortai.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.entity.ChatMessageVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final int MAX_MESSAGES = 10;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String convId = RedisConstants.CONV_ID_PREFIX + memoryId.toString();
        String key = RedisConstants.convMessagesKey(convId);
        List<String> jsonList = redis.opsForList().range(key, -MAX_MESSAGES, -1);
        if (jsonList == null || jsonList.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (String json : jsonList) {
            ChatMessage msg = deserialize(json);
            if (msg != null) {
                messages.add(msg);
            }
        }
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String convId = RedisConstants.CONV_ID_PREFIX + memoryId.toString();
        redis.delete(RedisConstants.convMessagesKey(convId));
    }

    private ChatMessage deserialize(String json) {
        try {
            ChatMessageVO vo = objectMapper.readValue(json, ChatMessageVO.class);
            if ("USER".equals(vo.getRole())) {
                return UserMessage.from(vo.getContent());
            } else if ("ASSISTANT".equals(vo.getRole())) {
                return AiMessage.from(vo.getContent());
            }
        } catch (JsonProcessingException e) {
            log.error("反序列化消息失败", e);
        }
        return null;
    }
}