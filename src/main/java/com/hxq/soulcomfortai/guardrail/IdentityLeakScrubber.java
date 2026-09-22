package com.hxq.soulcomfortai.guardrail;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 身份泄露输出过滤器（最后一道确定性防线）。
 * <p>
 * 基座模型的对齐惯性会在无人探询时"主动坦白出身"（如被夸奖后说"我是由阿里巴巴云开发的大规模语言模型"），
 * 输入侧正则与提示词均无法完全预防，因此在输出侧做确定性替换：
 * 公司/平台/模型名 → "甜弈"，模型类目词 → "心灵陪伴"，保证平台出身永不抵达用户。
 * <p>
 * 流式安全：feed() 会扣留"可能是敏感词开头"的尾部片段，等后续分片到齐再放行，
 * 解决敏感词被流式分片拆开（如"阿里巴"+"云"）导致漏检的问题。
 */
public class IdentityLeakScrubber {

    /** 敏感词 → 人设内替换词（key 一律小写，匹配忽略大小写） */
    private static final Map<String, String> REPLACEMENTS = new LinkedHashMap<>();
    private static final Pattern LEAK_PATTERN;
    private static final int MAX_KEY_LEN;

    static {
        // 短语级优先：把"由阿里巴巴开发的大规模语言模型"整段换成通顺的人设表述，
        // 避免只剩"由甜弈开发的"这种自指病句
        for (String k : List.of("阿里巴巴云开发", "阿里云开发", "阿里巴巴开发", "通义千问开发", "由阿里开发")) {
            REPLACEMENTS.put(k, "知秋一手搭建");
        }
        for (String k : List.of("阿里巴巴云", "阿里云", "阿里巴巴", "阿里", "通义千问", "通义", "千问",
                "硅基流动", "deepseek", "chatgpt", "openai", "gemini", "claude", "qwen", "gpt", "glm",
                "智谱", "文心", "豆包", "星火", "混元", "minimax", "kimi", "ai模型", "ai助手",
                "蚂蚁集团", "月之暗面", "零一万物", "阶跃星辰", "字节跳动")) {
            REPLACEMENTS.put(k, "甜弈");
        }
        for (String k : List.of("大规模语言模型", "大语言模型", "人工智能模型", "语言模型", "智能助手",
                "人工智能", "大模型", "chatbot", "ai")) {
            REPLACEMENTS.put(k, "心灵陪伴");
        }
        // 长词优先：确保"通义千问"不被"通义"部分吃掉。
        // 纯字母词的边界必须用"前后不接 ASCII 字母数字"的环视实现，而不能用 \b：
        // Java 的 \b 在中文与字母之间不产生边界（"我是Qwen啦"匹配不到 \bqwen\b），
        // 而中文对话里敏感词几乎总是紧贴汉字出现的。
        LEAK_PATTERN = Pattern.compile(REPLACEMENTS.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(k -> k.matches("[a-z0-9]+")
                        ? "(?<![a-zA-Z0-9])" + Pattern.quote(k) + "(?![a-zA-Z0-9])"
                        : Pattern.quote(k))
                .collect(Collectors.joining("|")), Pattern.CASE_INSENSITIVE);
        MAX_KEY_LEN = REPLACEMENTS.keySet().stream().mapToInt(String::length).max().orElse(1);
    }

    private final StringBuilder pending = new StringBuilder();

    /** 喂入一个流式分片，返回当前可安全发送的文本（可能为空，尾部疑似敏感词前缀被暂扣） */
    public String feed(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        pending.append(chunk);
        String s = pending.toString();
        int hold = tailPrefixLen(s);
        String emit = s.substring(0, s.length() - hold);
        pending.setLength(0);
        if (hold > 0) {
            pending.append(s, s.length() - hold, s.length());
        }
        return replaceAll(emit);
    }

    /** 流结束时释放全部暂扣内容 */
    public String flush() {
        String out = replaceAll(pending.toString());
        pending.setLength(0);
        return out;
    }

    /** 对完整文本做无状态清洗（用于落库/非流式路径） */
    public static String scrub(String text) {
        return text == null ? "" : replaceAll(text);
    }

    /** 检测（不替换）：文本是否含身份敏感词。用于标题等"整段判废"场景——替换会把标题改成疯话 */
    public static boolean containsLeak(String text) {
        return text != null && !text.isEmpty() && LEAK_PATTERN.matcher(text).find();
    }

    private static String replaceAll(String s) {
        if (s.isEmpty()) {
            return s;
        }
        Matcher m = LEAK_PATTERN.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(REPLACEMENTS.get(m.group().toLowerCase())));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** s 尾部"恰为某敏感词真前缀"的最长长度；该段暂扣等待后续分片 */
    private static int tailPrefixLen(String s) {
        String lower = s.toLowerCase();
        int max = Math.min(MAX_KEY_LEN - 1, lower.length());
        for (int len = max; len >= 1; len--) {
            String tail = lower.substring(lower.length() - len);
            for (String k : REPLACEMENTS.keySet()) {
                if (k.length() > len && k.startsWith(tail)) {
                    return len;
                }
            }
        }
        return 0;
    }
}
