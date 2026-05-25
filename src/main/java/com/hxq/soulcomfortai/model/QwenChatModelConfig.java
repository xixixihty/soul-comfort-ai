package com.hxq.soulcomfortai.model;


import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Data
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.chat-model")
public class QwenChatModelConfig {


    private String modelName;
    private String apiKey;

    @Resource
    private ChatModelListener chatModelListener;
    @Bean
    public ChatModel myQwenChatModel() {
        return QwenChatModel.builder()   // 补上 return
                .modelName(modelName)
                .apiKey(apiKey)
                .listeners(List.of((dev.langchain4j.model.chat.listener.ChatModelListener) chatModelListener))
                .build();
    }
}
