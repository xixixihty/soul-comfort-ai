package com.hxq.soulcomfortai.controller;

import com.hxq.soulcomfortai.ai.SoulComfortService;
import jakarta.annotation.Resource;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/soulComfort")
public class SoulComfortController {


    @Resource
    private SoulComfortService soulComfortService;


    @RequestMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(long memoryId, String message) {
        return soulComfortService.chatStream(memoryId, message)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }
}
