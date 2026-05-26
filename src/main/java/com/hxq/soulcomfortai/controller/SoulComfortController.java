package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.ai.SoulComfortService;
import com.hxq.soulcomfortai.service.ConversationService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/soulComfort")
public class SoulComfortController {

    private final SoulComfortService soulComfortService;
    private final ConversationService conversationService;

    public SoulComfortController(SoulComfortService soulComfortService,
                                 ConversationService conversationService) {
        this.soulComfortService = soulComfortService;
        this.conversationService = conversationService;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(
            @RequestParam String convId,
            @RequestParam(defaultValue = "default_user") String userId,
            @RequestParam String message) {

        long memoryId = parseConvIdToLong(convId);

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
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .doOnComplete(() ->
                        conversationService.appendAssistantMessage(convId, userId, fullResponse.toString()));

        return Flux.concat(thinkingFlux, chatFlux);
    }

    private long parseConvIdToLong(String convId) {
        return Long.parseLong(convId.substring(2));
    }
}