package com.hxq.soulcomfortai.ai;

import com.hxq.soulcomfortai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;


import java.util.List;

import static com.hxq.soulcomfortai.Constant.SystemPrompt.SYSTEM_PROMPT;


// @AiService
@InputGuardrails({SafeInputGuardrail.class})
public interface SoulComfortService {


    @SystemMessage(fromResource = "prompt/system_prompt.txt")
    String chat(String message);

    @SystemMessage(fromResource = "prompt/system_prompt.txt")
    String chatForReport(String message);

    /**
     * 会话标题生成专用：不带甜弈人设系统提示词（人设会让模型回出诗意句子和【情绪】标签，
     * 导致标题生成"看似调用成功、实则从未产出干净标题"），也不走对话记忆。
     */
    @SystemMessage("你是会话标题生成器。根据用户发给陪伴AI的第一条消息，输出一个10字以内、概括其主题的中文标题。"
            + "只输出标题本身：不要引号、标点、前缀（如“标题：”）、情绪标签或任何解释。")
    String chatForTitle(@UserMessage String userMessage);


    record Report(String name, List<String> suggestionList) {}

    @SystemMessage(fromResource = "prompt/system_prompt.txt")
    Result<String> chatWithRag(String userMessage);



    @SystemMessage(fromResource = "prompt/system_prompt.txt")
    Flux<String> chatStream(@MemoryId long memoryId,@UserMessage String message);
}
