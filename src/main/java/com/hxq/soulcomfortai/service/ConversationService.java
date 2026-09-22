package com.hxq.soulcomfortai.service;

import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.ai.SoulComfortService;
import com.hxq.soulcomfortai.config.PromptTemplateLoader;
import com.hxq.soulcomfortai.config.SoulRedisProperties;
import com.hxq.soulcomfortai.dto.response.ConversationVO;
import com.hxq.soulcomfortai.dto.response.PageResult;
import com.hxq.soulcomfortai.entity.ChatMessageVO;
import com.hxq.soulcomfortai.entity.Conversation;
import com.hxq.soulcomfortai.exception.BusinessException;
import com.hxq.soulcomfortai.guardrail.IdentityLeakScrubber;
import com.hxq.soulcomfortai.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final SoulComfortService soulComfortService;
    private final StringRedisTemplate redis;
    private final SoulRedisProperties soulRedisProperties;
    private final PromptTemplateLoader promptTemplateLoader;

    public ConversationService(ConversationRepository conversationRepository,
                               SoulComfortService soulComfortService,
                               StringRedisTemplate redis,
                               SoulRedisProperties soulRedisProperties,
                               PromptTemplateLoader promptTemplateLoader) {
        this.conversationRepository = conversationRepository;
        this.soulComfortService = soulComfortService;
        this.redis = redis;
        this.soulRedisProperties = soulRedisProperties;
        this.promptTemplateLoader = promptTemplateLoader;
    }

    public ConversationVO create(String userId, String title, String tag) {
        long now = System.currentTimeMillis();
        String id = conversationRepository.nextId();

        Conversation conv = Conversation.builder()
                .id(id)
                .userId(userId)
                .title(title != null && !title.isBlank() ? title : "新对话")
                .tag(tag)
                .createdAt(now)
                .updatedAt(now)
                .build();

        conversationRepository.save(conv);
        return toVO(conv);
    }

    public ConversationVO updateTag(String convId, String userId, String tag) {
        conversationRepository.updateTag(convId, userId, tag);
        return findById(convId, userId);
    }

    public PageResult<ConversationVO> listByTag(String userId, String tag, int page, int size) {
        PageResult<Conversation> result = conversationRepository.findByUserIdAndTag(userId, tag, page, size);
        List<ConversationVO> voList = result.getRecords().stream()
                .map(this::toVOWithoutMessages)
                .toList();
        return new PageResult<>(voList, result.getTotal(), page, size);
    }

    public List<String> getUserTags(String userId) {
        return conversationRepository.getUserTags(userId);
    }

    public ConversationVO findById(String convId, String userId) {
        Conversation conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new RuntimeException("对话不存在或已过期"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该对话");
        }
        return toVO(conv, conversationRepository.findMessages(convId, 0, 50));
    }

    public ConversationVO findByIdWithAllMessages(String convId, String userId) {
        Conversation conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new RuntimeException("对话不存在或已过期"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该对话");
        }
        return toVO(conv, conversationRepository.findMessages(convId));
    }

    public PageResult<ConversationVO> list(String userId, int page, int size) {
        PageResult<Conversation> result = conversationRepository.findByUserId(userId, page, size);
        return new PageResult<>(
                result.getRecords().stream().map(this::toVOWithoutMessages).toList(),
                result.getTotal(),
                result.getPage(),
                result.getSize()
        );
    }

    public ConversationVO rename(String convId, String userId, String title) {
        Conversation conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new RuntimeException("对话不存在或已过期"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该对话");
        }
        conversationRepository.updateTitle(convId, title);
        return findById(convId, userId);
    }

    public void delete(String convId, String userId) {
        conversationRepository.delete(convId, userId);
    }

    public void appendUserMessage(String convId, String userId, String content) {
        ChatMessageVO message = ChatMessageVO.builder()
                .id("m_" + System.currentTimeMillis())
                .role("USER")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
        conversationRepository.appendMessage(convId, userId, message);
        conversationRepository.updateConversationTimestamp(convId);
    }

    public void appendAssistantMessage(String convId, String userId, String content) {
        ChatMessageVO message = ChatMessageVO.builder()
                .id("m_" + System.currentTimeMillis())
                .role("ASSISTANT")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
        conversationRepository.appendMessage(convId, userId, message);
        conversationRepository.updateConversationTimestamp(convId);
    }

    /** 甜弈主动关怀消息：角色仍是 ASSISTANT（记忆/历史链路零改动），kind 仅供前端渲染"先开口"专属气泡 */
    public ChatMessageVO appendCareMessage(String convId, String userId, String content) {
        ChatMessageVO message = ChatMessageVO.builder()
                .id("m_" + System.currentTimeMillis())
                .role("ASSISTANT")
                .content(content)
                .kind("care")
                .timestamp(System.currentTimeMillis())
                .build();
        conversationRepository.appendMessage(convId, userId, message);
        conversationRepository.updateConversationTimestamp(convId);
        return message;
    }

    public void tryGenerateTitle(String convId, String userId) {
        conversationRepository.findById(convId).ifPresent(conv -> {
            if (!"新对话".equals(conv.getTitle())) {
                return;
            }
            List<ChatMessageVO> messages = conversationRepository.findMessages(convId, 0, 2);
            if (messages.size() < 2) {
                return;
            }
            String userMsg = messages.get(0).getContent();
            String title = null;
            try {
                title = validateModelTitle(soulComfortService.chatForTitle(userMsg));
            } catch (Exception e) {
                log.warn("模型生成标题失败，改用首条消息截取兜底 convId={}: {}", convId, e.getMessage());
            }
            // 确定性兜底：模型失败/输出为空/输出判废时直接截取用户第一句话，保证标题永不卡在"新对话"
            if (title == null || title.isBlank()) {
                title = cleanTitle(userMsg);
            }
            if (title != null && !title.isBlank()) {
                if (title.length() > 12) {
                    title = title.substring(0, 12) + "…";
                }
                conversationRepository.updateTitle(convId, title);
                log.info("自动生成对话标题 convId={} title={}", convId, title);
            }
        });
    }

    /** 对话开场信号：模型没在写标题，而是把用户消息当聊天直接作答（自我介绍/问候） */
    private static final Pattern TITLE_ANSWER_START =
            Pattern.compile("^(你好|您好|嗨|哈喽|我是|我叫|我会|很高兴|当然)");

    /**
     * 模型标题验收：只接受"像标题"的输出，否则整段判废、返回 null 走用户原话兜底。
     * 标题旁路是全项目唯一不经 IdentityLeakScrubber 清洗的落库文本，且对它做词替换只会
     * 产出"你好，我叫甜弈，是甜弈…"式的疯话，因此正确动作是拒绝而非修饰。
     */
    private String validateModelTitle(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.replaceAll("【情绪】\\s*\\w+", "").trim();
        if (t.isEmpty() || t.length() > 24) {
            return null;
        }
        if (TITLE_ANSWER_START.matcher(t).find() || IdentityLeakScrubber.containsLeak(t)) {
            log.warn("模型标题输出判废（回答体/身份泄露）: {}", t);
            return null;
        }
        return cleanTitle(t);
    }

    /** 清洗标题：剥离【情绪】标签、引号/书名框、"标题："前缀与换行，收敛为单行短文本 */
    private String cleanTitle(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.replaceAll("【情绪】\\s*\\w+", "")
                .replaceAll("^[\"'“”「『【\\s]*标题[:：]?\\s*", "")
                .replaceAll("[\\n\\r\"'“”‘’【】]+", "")
                .trim();
        return t.length() > 15 ? t.substring(0, 15) : t;
    }

    public Map<String, Object> revokeMessage(String convId, String userId, String messageId) {
        Conversation conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new RuntimeException("对话不存在或已过期"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该对话");
        }

        List<ChatMessageVO> messages = conversationRepository.findMessages(convId);
        ChatMessageVO target = null;
        int index = -1;

        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getId())) {
                target = messages.get(i);
                index = i;
                break;
            }
        }

        if (target == null) {
            throw new RuntimeException("消息不存在");
        }

        if (!"USER".equals(target.getRole())) {
            throw new RuntimeException("只能撤回自己的消息");
        }

        target.setRevoked(true);
        target.setContent("");
        messages.set(index, target);
        conversationRepository.saveMessages(convId, userId, messages);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messageId", messageId);
        result.put("revoked", true);
        result.put("displayContent", "此消息已被撤回");
        result.put("revokedAt", System.currentTimeMillis());
        return result;
    }

    public Map<String, Object> editMessage(String convId, String userId, String messageId, String newContent) {
        Conversation conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new RuntimeException("对话不存在或已过期"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该对话");
        }

        List<ChatMessageVO> messages = conversationRepository.findMessages(convId);
        ChatMessageVO target = null;
        int index = -1;

        for (int i = 0; i < messages.size(); i++) {
            if (messageId.equals(messages.get(i).getId())) {
                target = messages.get(i);
                index = i;
                break;
            }
        }

        if (target == null) {
            throw new RuntimeException("消息不存在");
        }

        if (!"USER".equals(target.getRole())) {
            throw new RuntimeException("只能编辑自己的消息");
        }

        ChatMessageVO.EditRecord editRecord = ChatMessageVO.EditRecord.builder()
                .content(target.getContent())
                .editedAt(System.currentTimeMillis())
                .build();

        List<ChatMessageVO.EditRecord> editHistory = target.getEditHistory();
        if (editHistory == null) {
            editHistory = new ArrayList<>();
        }
        editHistory.add(editRecord);

        target.setContent(newContent);
        target.setEditHistory(editHistory);
        messages.set(index, target);
        conversationRepository.saveMessages(convId, userId, messages);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messageId", messageId);
        result.put("content", newContent);
        result.put("editHistory", editHistory);
        return result;
    }

    public Map<String, Object> getSuggestedQuestions(String convId, String userId) {
        Conversation conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new RuntimeException("对话不存在或已过期"));
        if (!conv.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该对话");
        }

        String prompt = promptTemplateLoader.render("suggested_questions",
                Map.of("conversation_summary", "最近的对话",
                       "current_emotion", "未知"));

        String response = soulComfortService.chatForReport(prompt);
        List<String> questions = Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(q -> !q.isEmpty())
                .limit(3)
                .toList();

        String cacheKey = RedisConstants.suggestedQuestionsKey(convId, "latest");
        redis.opsForValue().set(cacheKey, String.join("\n", questions), 30, TimeUnit.MINUTES);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("questions", questions);
        return data;
    }

    private ConversationVO toVO(Conversation conv) {
        return toVO(conv, conversationRepository.findMessages(conv.getId()));
    }

    private ConversationVO toVO(Conversation conv, java.util.List<ChatMessageVO> messages) {
        return ConversationVO.builder()
                .id(conv.getId())
                .userId(conv.getUserId())
                .title(conv.getTitle())
                .tag(conv.getTag())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .messages(messages)
                .build();
    }

    private ConversationVO toVOWithoutMessages(Conversation conv) {
        return ConversationVO.builder()
                .id(conv.getId())
                .userId(conv.getUserId())
                .title(conv.getTitle())
                .tag(conv.getTag())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .messages(null)
                .build();
    }
}