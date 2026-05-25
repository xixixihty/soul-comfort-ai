package com.hxq.soulcomfortai;

import com.hxq.soulcomfortai.ai.SoulComfortAI;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SoulComfortAiApplicationTests {
    @Resource
    private SoulComfortAI soulComfortAI;


    @Test
    void chat() {
        soulComfortAI.chat("你好");
    }

    @Test
    void chat2() {
        UserMessage userMessage = UserMessage.from(
                TextContent.from("描述图片"),
                ImageContent.from("")
        );
        soulComfortAI.chatWithMessage(userMessage);
    }
}
