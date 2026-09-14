package com.hxq.soulcomfortai.mcp;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class McpConfig {
    @Value("${bigmodel.api-key}")
    private String apiKey;

    /**
     * 智谱 web-search MCP 工具提供者。
     * <p>
     * 其 open.bigmodel.cn SSE 通道不稳定，曾在流式聊天期间超时/重置，导致整条回答中断。
     * 默认关闭（mcp.enabled=false 时不注册本 Bean）；开启时设置传输层与工具执行超时，避免长时间挂起。
     * <p>
     * 注意：langchain4j-mcp 1.1.0-beta7 的 McpToolProvider.Builder.build() 不允许空 mcpClients
     * （内部 new CopyOnWriteArrayList<>(null) 会 NPE），因此关闭时不能返回"空提供者"，
     * 改为直接不注册 Bean，由 SoulComfortServiceFactory 的 ObjectProvider 感知是否存在。
     */
    @Bean
    @ConditionalOnProperty(name = "mcp.enabled", havingValue = "true")
    public McpToolProvider mcpTooLProvider(@Value("${mcp.timeout-seconds:15}") long timeoutSeconds) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + apiKey)
                .timeout(timeout)
                .logRequests(false)
                .logResponses(false)
                .build();
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .key("yupiMcpclient")
                .transport(transport)
                .toolExecutionTimeout(timeout)
                .build();
        return McpToolProvider
                .builder()
                .mcpClients(mcpClient)
                .build();
    }
}