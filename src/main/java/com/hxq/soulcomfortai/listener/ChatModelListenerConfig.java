package com.hxq.soulcomfortai.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelListenerConfig {
    @Bean
    public dev.langchain4j.model.chat.listener.ChatModelListener chatModelListener() {
        return new dev.langchain4j.model.chat.listener.ChatModelListener() {
            private static final Logger log = LoggerFactory.getLogger(ChatModelListener.class);

            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                log.info("onRequest: {}", requestContext);
            }

            @Override
            public void onResponse(ChatModelResponseContext responseContext) {  // 注意参数类型不同
                log.info("onResponse: {}", responseContext);
            }

            @Override
            public void onError(ChatModelErrorContext errorContext) {           // 注意参数类型不同
                log.error("onError: {}, {}", errorContext, errorContext.error().getMessage());
            }
        };
    }
}
