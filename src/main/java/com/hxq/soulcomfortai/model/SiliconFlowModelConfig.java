package com.hxq.soulcomfortai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * 硅基流动(SiliconFlow)大模型配置类：OpenAI 兼容端点，手动装配 ChatModel / StreamingChatModel / EmbeddingModel。
 * <p>
 * 统一读取 siliconflow.* 配置（api-key 走环境变量 SILICONFLOW_API_KEY，避免明文入库）。
 */
@Configuration
public class SiliconFlowModelConfig {

    @Resource
    private ChatModelListener chatModelListener;

    @Bean
    public ChatModel siliconflowChatModel(@Value("${siliconflow.base-url}") String baseUrl,
                                          @Value("${siliconflow.chat-model}") String modelName,
                                          @Value("${siliconflow.api-key}") String apiKey) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                // 采样多样性：温度 0.9 + presence_penalty 0.3，
                // 同一语义问题多次提问能得到不同措辞，避免 0.7 下的模板化收敛
                .temperature(0.9)
                .presencePenalty(0.3)
                // 回复纪律 30-120 字：400 token 物理上写不下八股攻略清单，从源头掐断长篇助手腔
                .maxTokens(400)
                .timeout(Duration.ofSeconds(60))
                .logRequests(false)
                .logResponses(false)
                .listeners(List.of(chatModelListener))
                .build();
    }

    /**
     * 身份语义哨兵专用模型：温度固定 0、短输出上限，
     * 保证 IdentityGuard 的 true/false 分类判定稳定可靠，不受对话采样波动影响。
     */
    @Bean
    public ChatModel siliconflowGuardrailChatModel(@Value("${siliconflow.base-url}") String baseUrl,
                                                   @Value("${siliconflow.chat-model}") String modelName,
                                                   @Value("${siliconflow.api-key}") String apiKey) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                .temperature(0.0)
                .maxTokens(256)
                .timeout(Duration.ofSeconds(30))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    public StreamingChatModel siliconflowStreamingChatModel(@Value("${siliconflow.base-url}") String baseUrl,
                                                            @Value("${siliconflow.streaming-chat-model}") String modelName,
                                                            @Value("${siliconflow.api-key}") String apiKey) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                .temperature(0.9)
                .presencePenalty(0.3)
                // 与主对话模型一致：短输出上限，物理阻断攻略式长篇
                .maxTokens(400)
                .logRequests(false)
                .logResponses(false)
                .listeners(List.of(chatModelListener))
                .build();
    }

    @Bean
    public EmbeddingModel siliconflowEmbeddingModel(@Value("${siliconflow.base-url}") String baseUrl,
                                                    @Value("${siliconflow.embedding-model}") String modelName,
                                                    @Value("${siliconflow.api-key}") String apiKey) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                // 长时间空闲后 keep-alive 连接会被服务端单方面关闭，复用即 Connection reset；
                // 重试会新建连接，3 次基本可自愈（此前 1 次太低，一次抖动就降级丢知识库）
                .maxRetries(3)
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}