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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        try {
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
        } catch (RuntimeException e) {
            // Redis 不可用时降级：本次对话无历史记忆，不影响聊天主流程
            log.warn("读取对话记忆失败，本次对话将无历史记忆：{}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // 会话记忆（数字 convId）只以 ConversationService 的追加写入为权威来源（同 key、带 id/timestamp、已裁剪窗口），
        // 此处若再用内存快照全量覆盖会：①丢 id/时间戳；②产生 USER,USER,ASSISTANT,ASSISTANT 重复对；
        // ③把标题生成等旁路 prompt 污染进对话历史。因此会话 key 跳过覆盖写。
        // 但旁路对话（memoryId="default"：风格哨兵同步重生成、标题生成、日记共鸣、欢迎语等无 @MemoryId 的方法）
        // 没有追加写入链路——若也跳过持久化，MessageWindowChatMemory.messages() 每次从 store 重读都为空，
        // 模型请求会瞬间报 "messages cannot be null or empty"。故 default 快照照常全量覆盖。
        if (!"default".equals(String.valueOf(memoryId))) {
            log.debug("updateMessages 跳过覆盖写 memoryId={} messages={}", memoryId, messages.size());
            return;
        }
        String key = RedisConstants.convMessagesKey(RedisConstants.CONV_ID_PREFIX + memoryId);
        try {
            List<String> jsonList = new ArrayList<>();
            for (ChatMessage msg : messages) {
                ChatMessageVO vo = toVO(msg);
                if (vo != null) {
                    jsonList.add(objectMapper.writeValueAsString(vo));
                }
            }
            redis.delete(key);
            if (!jsonList.isEmpty()) {
                redis.opsForList().rightPushAll(key, jsonList);
                redis.expire(key, RedisConstants.DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("default 对话记忆持久化失败，已忽略：{}", e.getMessage());
        }
    }

    /** 只持久化纯文本的 USER/ASSISTANT 消息；SYSTEM、工具调用等结构化消息跳过 */
    private ChatMessageVO toVO(ChatMessage msg) {
        if (msg instanceof UserMessage user) {
            if (!user.hasSingleText()) {
                return null;
            }
            return ChatMessageVO.builder().role("USER").content(user.singleText())
                    .timestamp(System.currentTimeMillis()).build();
        }
        if (msg instanceof AiMessage ai) {
            if (ai.text() == null || ai.text().isBlank()) {
                return null;
            }
            return ChatMessageVO.builder().role("ASSISTANT").content(ai.text())
                    .timestamp(System.currentTimeMillis()).build();
        }
        return null;
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String convId = RedisConstants.CONV_ID_PREFIX + memoryId.toString();
        try {
            redis.delete(RedisConstants.convMessagesKey(convId));
        } catch (RuntimeException e) {
            log.warn("删除对话记忆失败，已忽略：{}", e.getMessage());
        }
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