package com.hxq.soulcomfortai.listener;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class ChatModelListenerConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatModelListenerConfig.class);
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    @Bean
    public dev.langchain4j.model.chat.listener.ChatModelListener chatModelListener() {
        return new dev.langchain4j.model.chat.listener.ChatModelListener() {

            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                String traceId = UUID.randomUUID().toString().substring(0, 8);
                TRACE_ID.set(traceId);
                int msgCount = requestContext.chatRequest().messages().size();
                log.info("[traceId={}] LLM request | model={} | msgCount={}",
                        traceId, requestContext.modelProvider(), msgCount);
            }

            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                String traceId = TRACE_ID.get();
                log.info("[traceId={}] LLM response | model={} | tokenUsage={}",
                        traceId, responseContext.modelProvider(),
                        responseContext.chatResponse().metadata().tokenUsage());
                TRACE_ID.remove();
            }

            @Override
            public void onError(ChatModelErrorContext errorContext) {
                String traceId = TRACE_ID.get();
                log.error("[traceId={}] LLM error | model={} | error={}",
                        traceId, errorContext.modelProvider(), errorContext.error().getMessage());
                TRACE_ID.remove();
            }
        };
    }
}
