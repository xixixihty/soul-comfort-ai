package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.ai.SoulComfortService;
import com.hxq.soulcomfortai.exception.BusinessException;
import com.hxq.soulcomfortai.service.ConversationService;
import com.hxq.soulcomfortai.service.EmotionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/soulComfort")
public class SoulComfortController {

    private static final Logger log = LoggerFactory.getLogger(SoulComfortController.class);

    private static final Pattern EMOTION_TAG_PATTERN = Pattern.compile("【情绪】\\w+\\s*");

    private final SoulComfortService soulComfortService;
    private final ConversationService conversationService;
    private final EmotionService emotionService;

    public SoulComfortController(SoulComfortService soulComfortService,
                                 ConversationService conversationService,
                                 EmotionService emotionService) {
        this.soulComfortService = soulComfortService;
        this.conversationService = conversationService;
        this.emotionService = emotionService;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestParam String convId,
            @RequestAttribute("userId") String userId,
            @RequestParam String message,
            @RequestParam(required = false) String quoteMessage,
            @RequestParam(required = false) String quoteRole) {

        SseEmitter emitter = new SseEmitter(300_000L);

        long memoryId;
        try {
            memoryId = parseConvIdToLong(convId);
        } catch (IllegalArgumentException e) {
            log.warn("无效的 convId: {}", convId);
            emitter.completeWithError(new BusinessException(400, "对话ID格式无效"));
            return emitter;
        }

        conversationService.appendUserMessage(convId, userId, message);

        String fullMessage = message;
        if (quoteMessage != null && !quoteMessage.isBlank()) {
            String label = "assistant".equals(quoteRole) ? "甜弈" : "用户";
            fullMessage = String.format("[引用%s的消息：「%s」]\n\n%s", label, quoteMessage, message);
        }

        StringBuilder fullResponse = new StringBuilder();
        Flux<String> rawFlux = soulComfortService.chatStream(memoryId, fullMessage);

        emitter.onCompletion(() -> {
            String cleanResponse = EMOTION_TAG_PATTERN.matcher(fullResponse.toString()).replaceAll("");
            if (!cleanResponse.isBlank()) {
                conversationService.appendAssistantMessage(convId, userId, cleanResponse);
            } else {
                String fallback = "抱歉，AI暂时无法回复，请稍后再试... 🌿";
                conversationService.appendAssistantMessage(convId, userId, fallback);
                log.warn("AI流式响应无有效内容，已保存降级消息 convId={}", convId);
            }
            emotionService.tryRecordEmotion(userId, convId, fullResponse.toString());
            conversationService.tryGenerateTitle(convId, userId);
        });

        emitter.onError(throwable -> {
            log.error("SSE连接异常 convId={}", convId, throwable);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时 convId={}", convId);
        });

        try {
            sendSseEvent(emitter, "status", "🌿 甜弈正在感受你的心情...");
        } catch (IOException e) {
            log.warn("发送思考状态失败 convId={}", convId, e);
            emitter.complete();
            return emitter;
        }

        rawFlux.subscribe(
                chunk -> {
                    try {
                        String cleanChunk = EMOTION_TAG_PATTERN.matcher(chunk).replaceAll("");
                        fullResponse.append(chunk);
                        sendSseEvent(emitter, null, cleanChunk);
                    } catch (IOException e) {
                        log.warn("发送流式数据失败 convId={}", convId, e);
                        emitter.complete();
                    }
                },
                error -> {
                    log.error("AI流式响应异常 convId={}, 已累积: {}字符", convId, fullResponse.length(), error);
                    String fallback = "抱歉，连接出了点问题，请稍后再试... 🌿";
                    fullResponse.append(fallback);
                    try {
                        sendSseEvent(emitter, null, fallback);
                    } catch (IOException ex) {
                        log.warn("发送降级消息失败 convId={}", convId, ex);
                    }
                    emitter.complete();
                },
                emitter::complete
        );

        return emitter;
    }

    private void sendSseEvent(SseEmitter emitter, String event, String data) throws IOException {
        SseEmitter.SseEventBuilder builder = SseEmitter.event();
        if (event != null) {
            builder.name(event);
        }
        builder.data(data);
        emitter.send(builder);
    }

    private long parseConvIdToLong(String convId) {
        return Long.parseLong(convId.substring(2));
    }
}