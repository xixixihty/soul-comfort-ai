package com.hxq.soulcomfortai.guardrail;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 身份探询哨兵：用模型语义判定"用户是否在询问 AI 的身份/出身/模型平台/开发者"。
 * 正则黑名单（枚举问法）永远有漏网之鱼（如"你是deepseek还是Qwen"），
 * 此哨兵对任意措辞都能理解语义，命中则直接返回样板回复，杜绝模型自报家门。
 * 复用主对话模型（免费），仅多一次轻量调用；判定失败时容忍降级为非身份。
 */
@Component
public class IdentityGuard {

    private static final Logger log = LoggerFactory.getLogger(IdentityGuard.class);

    /** 明显是知识科普类提问时跳过判定，避免误伤（如"请解释什么是大模型"） */
    private static final String COMMON_KNOWLEDGE_FILTER =
            "解释|讲解|科普|介绍一下什么是|原理|定义|区别|对比|教程|学习|课程|考试|作业|翻译|帮我写|总结|生成";
    /** 明显是闲聊/倾诉等非身份场景的语义特征，命中后也跳过判定 */
    private static final String NON_PROBE_FILTER =
            "睡觉|晚安|早安|吃饭|饿|累|难过|伤心|分手|失恋|工作|考试|压力|焦虑|心情|想哭|烦|emo|哭";

    /**
     * 身份探询信号预筛：命中才继续走模型语义判定；未命中直接放行（普通聊天零模型调用）。
     * 覆盖"你是/你是谁/谁开发/诞生/公司/模型"等身份关键词 + 常见模型平台点名（deepseek/Qwen/GPT…）。
     * 例："你是deepseek还是Qwen呢？"命中"你是"+"deepseek"，进入语义判定后被拦截。
     */
    private static final Pattern PROBE_SIGNAL = Pattern.compile(
            "你是|你是谁|你叫|名字|来自|属于|你是哪|哪个公司|哪家公司|什么公司|哪个模型|哪个平台|"
                    + "谁开发|谁创造|谁做|谁造|谁养|谁负责|谁训练|谁孕育|开发你|创造你|训练你|养着你|"
                    + "你的开发者|你的创造者|你的创始人|你的老板|诞生|从哪里来|怎么来的|哪里来|"
                    + "称呼|怎么叫|该怎么叫|该如何叫|叫什么|管你叫|"
                    + "身份|背景|出身|deepseek|qwen|gpt|chatgpt|claude|gemini|文心|通义|豆包|kimi|"
                    + "星火|glm|智谱|混元|minimax|千问|doubao|大模型|AI模型|语言模型|机器人|人工智能");

    private final ChatModel chatModel;

    public IdentityGuard(@Qualifier("siliconflowGuardrailChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 判定用户消息是否为"身份/出身/模型平台/开发者"类探询。
     * 两级流水线：廉价预筛（正则命中才继续，普通聊天零开销）→ 模型语义判定（容忍降级）。
     */
    public boolean isIdentityProbe(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        if (message.length() > 80) {
            return false;
        }
        // 第一级：无身份信号的消息直接放行，不触发模型调用
        if (!PROBE_SIGNAL.matcher(message).find()) {
            return false;
        }
        // 含明确科普/非身份意图的关键词时直接放行，减少误伤与调用
        if (message.matches(".*(" + COMMON_KNOWLEDGE_FILTER + ").*")
                && message.matches(".*(" + NON_PROBE_FILTER + ").*")) {
            return false;
        }
        try {
            String prompt = "你是内容安全助手。判断下面的【用户提问】是否在询问\"AI的身份、出身、由谁/哪家公司开发、属于哪个模型或平台\"。"
                    + "（例如：你是谁开发的、你是哪个AI、你是什么模型、你是deepseek还是Qwen、你是GPT吗、你的创造者是谁）。"
                    + "若属于这类身份探询，输出 true；若是普通聊天、情绪倾诉、知识科普、技术问题或其他内容，输出 false。"
                    + "只允许输出 true 或 false，不要输出其他任何内容。\n"
                    + "【用户提问】" + message;
            String resp = chatModel.chat(prompt);
            boolean probe = resp != null && resp.trim().toLowerCase().contains("true");
            if (probe) {
                log.info("[身份哨兵] 判定为身份探询 message={}", message);
            }
            return probe;
        } catch (Exception e) {
            // 判定链路异常时不阻塞聊天，按非身份处理
            log.warn("[身份哨兵] 判定失败，按非身份处理: {}", e.getMessage());
            return false;
        }
    }
}