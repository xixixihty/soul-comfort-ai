package com.hxq.soulcomfortai.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import static com.hxq.soulcomfortai.Constant.SystemPrompt.SYSTEM_PROMPT;

@Service
@Slf4j
public class SoulComfortAI {

    @Resource
    private ChatModel qwenChatModel;



    public String chat(String message) {
        SystemMessage systemMessage = SystemMessage.from(SYSTEM_PROMPT);
        UserMessage userMessage = UserMessage.from(message);
        ChatResponse chat = qwenChatModel.chat(systemMessage, userMessage);
        AiMessage aiMessage = chat.aiMessage();
        log.info("AI 输出" + aiMessage.toString());
        return aiMessage.text();
    }

    public String chatWithMessage(UserMessage userMessage) {
        ChatResponse chat = qwenChatModel.chat(userMessage);
        AiMessage aiMessage = chat.aiMessage();
        log.info("AI 输出" + aiMessage.toString());
        return aiMessage.text();
    }
}
