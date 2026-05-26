package com.hxq.soulcomfortai.ai;

import com.hxq.soulcomfortai.store.RedisChatMemoryStore;
import com.hxq.soulcomfortai.tools.SoulConformTools;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SoulComfortServiceFactory {

    @Resource
    private ChatModel myQwenChatModel;

    @Resource
    private ContentRetriever contentRetriever;
    @Resource
    private McpToolProvider mcpToolProvider;
    @Resource
    private StreamingChatModel qwenStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Bean
    public SoulComfortService createSoulComfortService() {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(10)
                .build();

        SoulComfortService soulComfortService = AiServices.builder(SoulComfortService.class)
                .chatModel(myQwenChatModel)
                .streamingChatModel(qwenStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .chatMemoryStore(redisChatMemoryStore)
                        .id(memoryId)
                        .maxMessages(10)
                        .build())
                .contentRetriever(contentRetriever)
                .tools(new SoulConformTools())
                .toolProvider(mcpToolProvider)
                .build();
        return soulComfortService;
    }
}