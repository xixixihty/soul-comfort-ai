package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.ai.SoulComfortService;
import com.hxq.soulcomfortai.exception.BusinessException;
import com.hxq.soulcomfortai.service.ConversationService;
import com.hxq.soulcomfortai.service.EmotionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

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
    public Flux<ServerSentEvent<String>> chat(
            @RequestParam String convId,
            @RequestAttribute("userId") String userId,
            @RequestParam String message) {

        long memoryId;
        try {
            memoryId = parseConvIdToLong(convId);
        } catch (IllegalArgumentException e) {
            log.warn("无效的 convId: {}", convId);
            return Flux.error(new BusinessException(400, "对话ID格式无效"));
        }

        conversationService.appendUserMessage(convId, userId, message);

        Flux<ServerSentEvent<String>> thinkingFlux = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("status")
                        .data("\uD83C\uDF3F 甜弈正在感受你的心情...")
                        .build()
        );

        StringBuilder fullResponse = new StringBuilder();
        Flux<String> rawFlux = soulComfortService.chatStream(memoryId, message);

        Flux<ServerSentEvent<String>> chatFlux = rawFlux
                .doOnNext(fullResponse::append)
                .map(chunk -> {
                    String cleanChunk = EMOTION_TAG_PATTERN.matcher(chunk).replaceAll("");
                    return ServerSentEvent.<String>builder()
                            .data(cleanChunk)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("AI流式响应异常 convId={}, 已累积: {}字符", convId, fullResponse.length(), e);
                    String fallback = "抱歉，连接出了点问题，请稍后再试... 🌿";
                    fullResponse.append(fallback);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .data(fallback)
                            .build());
                })
                .doFinally(signalType -> {
                    String cleanResponse = EMOTION_TAG_PATTERN.matcher(fullResponse.toString()).replaceAll("");
                    if (!cleanResponse.isBlank()) {
                        conversationService.appendAssistantMessage(convId, userId, cleanResponse);
                    } else {
                        String fallback = "抱歉，AI暂时无法回复，请稍后再试... 🌿";
                        conversationService.appendAssistantMessage(convId, userId, fallback);
                        log.warn("AI流式响应无有效内容，已保存降级消息 convId={}, signalType={}", convId, signalType);
                    }
                    emotionService.tryRecordEmotion(userId, convId, fullResponse.toString());
                });

        return Flux.concat(thinkingFlux, chatFlux);
    }

    private long parseConvIdToLong(String convId) {
        return Long.parseLong(convId.substring(2));
    }
}