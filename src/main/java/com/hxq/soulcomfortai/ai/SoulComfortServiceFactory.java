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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SoulComfortServiceFactory {

    @Resource
    private ChatModel siliconflowChatModel;

    @Resource
    private ObjectProvider<ContentRetriever> contentRetrieverProvider;
    @Resource
    private ObjectProvider<McpToolProvider> mcpToolProviderProvider;
    @Resource
    private StreamingChatModel siliconflowStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Bean
    public SoulComfortService createSoulComfortService(@Value("${mcp.enabled:false}") boolean mcpEnabled) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(10)
                .build();

        AiServices<SoulComfortService> builder = AiServices.builder(SoulComfortService.class)
                .chatModel(siliconflowChatModel)
                .streamingChatModel(siliconflowStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .chatMemoryStore(redisChatMemoryStore)
                        .id(memoryId)
                        .maxMessages(10)
                        .build())
                .tools(new SoulConformTools());

        // 仅当 MCP 启用时才挂载工具提供者，避免不稳定的智谱 SSE 通道级联影响流式回答
        if (mcpEnabled) {
            McpToolProvider mcpToolProvider = mcpToolProviderProvider.getIfAvailable();
            if (mcpToolProvider != null) {
                builder.toolProvider(mcpToolProvider);
            }
        }

        // RAG 可用时注入内容检索器，不可用（如摄入失败）时降级为纯对话模式
        ContentRetriever contentRetriever = contentRetrieverProvider.getIfAvailable();
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
        }
        return builder.build();
    }
}