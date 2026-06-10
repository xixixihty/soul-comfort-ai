package com.hxq.soulcomfortai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "soul.redis")
public class SoulRedisProperties {

    private int ttlDays = 30;
    private int maxMessages = 500;

    public long getTtlSeconds() {
        return (long) ttlDays * 24 * 60 * 60;
    }
}