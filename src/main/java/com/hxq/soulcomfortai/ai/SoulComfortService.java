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


    record Report(String name, List<String> suggestionList) {}

    @SystemMessage(fromResource = "prompt/system_prompt.txt")
    Result<String> chatWithRag(String userMessage);



    @SystemMessage(fromResource = "prompt/system_prompt.txt")
    Flux<String> chatStream(@MemoryId long memoryId,@UserMessage String message);
}
