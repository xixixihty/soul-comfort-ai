package com.hxq.soulcomfortai.guardrail;

import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 两段式生成的第一段：让温度 0 的哨兵模型先"诊"再"答"。
 * <p>
 * 单次生成时，模型要同时做情绪识别、组织语言、守格式，注意力被摊薄，容易滑向
 * 通用助手腔（攻略清单）。此处先用轻量模型输出一行内观结论（情绪/痛点/安慰方向），
 * 拼进用户消息作为"已完成的分析"，主模型便只需专注于按方向低声安慰。
 * <p>
 * 该段仅存在于发往模型的输入，不落入会话历史；失败时返回空串静默降级，绝不阻塞聊天。
 */
@Component
public class EmotionInsight {

    private static final Logger log = LoggerFactory.getLogger(EmotionInsight.class);

    private final ChatModel chatModel;

    public EmotionInsight(@Qualifier("siliconflowGuardrailChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /** 分析超长的输入没有情绪挖掘价值（多为粘贴的长文本），直接跳过省一次调用 */
    private static final int MAX_INPUT_LEN = 300;

    /**
     * 产出形如"情绪=焦虑｜痛点=怕准备不完、怕辜负期待｜方向=先接住慌乱，陪他把今天缩小成一件事"的一行内观。
     * 任何异常/超长/空输入均返回 ""，调用方按无内观处理。
     */
    public String analyze(String message) {
        if (message == null || message.isBlank() || message.length() > MAX_INPUT_LEN) {
            return "";
        }
        try {
            String prompt = "你是心理咨询师。读下面这位用户的这句话，判断他压在话底下的真实情绪和最痛的心结。"
                    + "只输出一行，格式严格为：情绪=xx｜痛点=xx｜方向=xx。"
                    + "情绪限填：难过/焦虑/迷茫/愤怒/开心/平静；"
                    + "痛点用一句话说清他真正在怕什么、委屈什么、失去什么，不要复述原话；"
                    + "方向给一句'先接住什么情绪、再怎样轻声陪伴'的建议，禁止给行动清单。"
                    + "不要输出其他任何内容。\n【用户的话】" + message;
            String resp = chatModel.chat(prompt);
            if (resp == null) {
                return "";
            }
            String insight = resp.replace("\r", "").replace("\n", " ").trim();
            int start = insight.indexOf("情绪=");
            if (start < 0) {
                start = insight.indexOf("痛点=");
                if (start < 0) {
                    log.warn("[内观哨兵] 输出格式异常，已忽略: {}", insight);
                    return "";
                }
            }
            insight = insight.substring(start);
            if (insight.length() > 120) {
                insight = insight.substring(0, 120);
            }
            return insight;
        } catch (Exception e) {
            log.warn("[内观哨兵] 分析失败，按无内观继续: {}", e.getMessage());
            return "";
        }
    }
}
