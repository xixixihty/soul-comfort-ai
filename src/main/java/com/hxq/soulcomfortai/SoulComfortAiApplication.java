package com.hxq.soulcomfortai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SoulComfortAiApplication {

    public static void main(String[] args) {
        // JDK HttpClient 连接池空闲保活上限 30s（默认约 1200s）：
        // 长时间空闲后不再复用已被服务端单方面关闭的 keep-alive 连接，
        // 从源头消除"空闲后第一个请求 Connection reset"（须在首个 HttpClient 创建前设置）
        System.setProperty("jdk.httpclient.keepalive.timeout", "30");
        SpringApplication.run(SoulComfortAiApplication.class, args);
    }

}
