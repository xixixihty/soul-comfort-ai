package com.hxq.soulcomfortai.service.care;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxq.soulcomfortai.Constant.RedisConstants;
import com.hxq.soulcomfortai.ai.SoulComfortService;
import com.hxq.soulcomfortai.entity.ChatMessageVO;
import com.hxq.soulcomfortai.guardrail.IdentityLeakScrubber;
import com.hxq.soulcomfortai.service.ConversationService;
import com.hxq.soulcomfortai.service.care.EmotionTimelineService.EmotionPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 主动关怀编排（情绪灯塔）：读趋势 → 判时机 → 生成/兜底文案 → 记账频控。
 * <p>
 * 隐私红线在本类有三道闸：
 * 1. 情绪数据只经 EmotionTimelineService 进入（note 已在漏斗处物理丢弃）；
 * 2. 给模型的 prompt 只含"日期+心情词"，且禁止模型引用用户原话（校验正则拦截）；
 * 3. 关怀文案落选/超纲时回退到人工撰写的兜底池，绝不透传模型原文。
 */
@Slf4j
@Service
public class ProactiveCareService {

    /** 待展示的关怀卡：type = trend（趋势回访）| anniversary（去年今日）| star（解忧纸星） */
    public record CareCard(String careId, String type, String text, long createdAt) {}

    private static final Map<String, String> EMOTION_WORDS = Map.of(
            "happy", "开心", "calm", "平静", "sad", "难过",
            "anxious", "焦虑", "angry", "生气", "energetic", "有活力",
            "tired", "疲惫", "grateful", "感恩");

    /** 校验不过时的兜底文案池（人工撰写，只引用情绪词） */
    private static final Map<String, List<String>> FALLBACK_POOLS = Map.of(
            "TREND_DOWN", List.of(
                    "最近的日子好像有点沉，我在呢，想说什么都可以从这里开始。",
                    "感觉到你这几天的心情在往下走，不用急着好起来，先让我陪一会儿。"),
            "PERSISTENT_LOW", List.of(
                    "这段时间不容易，你一直撑着，真的很辛苦了。今天想聊聊吗？",
                    "连续的疲惫也要有出口，我把灯留着，你随时可以进来坐坐。"),
            "RECOVERY", List.of(
                    "感觉到你的心情在慢慢放晴，替你高兴，想听听是什么让你松了下来。",
                    "最近的你比前阵子亮了一点，这种回暖值得被记住。"));
    /**  anniversaries 按情绪分的静态池（去年今日，不经过模型） */
    private static final Map<String, List<String>> ANNIVERSARY_POOLS = Map.of(
            "sad", List.of("去年今天的你有些难过，现在的你想跟那时候的自己说点什么吗？",
                    "一年前的今天你有些低落，不知道现在的你，是否已经好一些了。"),
            "anxious", List.of("去年今天的你带着焦虑签到，想回头看看这一年，哪些事其实没有想象中可怕？",
                    "一年前的今天你有些不安。这一路走过来，也许值得和那时候的自己聊聊。"),
            "angry", List.of("去年今天的你有些生气。现在回想，那团火后来是怎么慢慢熄掉的？",
                    "一年前的今天你签下了生气，不知道那件事后来怎么样了。"),
            "tired", List.of("去年今天的你很疲惫。这一年过去，希望你学会了多疼自己一点。",
                    "一年前的今天你带着疲惫签到，现在的你休息得好一些了吗？"));

    private static final Pattern EMOTION_TAG = Pattern.compile("【情绪】\\w+\\s*");
    private static final Pattern SONG_TAG = Pattern.compile("【点歌】[^\\n]*\\n?");
    /** 模型若"引用用户原话"即越界：你当时说 / 你那时说 / 你说过 */
    private static final Pattern QUOTE_PATTERNS = Pattern.compile("你(当时|那时|之前|上次)说过?|清单|首先|其次|建议你|你可以(尝试|试试)|请(尝试|务必要?)");
    private static final Pattern NUMBERED_LIST = Pattern.compile("(?m)^\\s*(?:\\d+[.、)]|[-•])\\s");

    private static final String F_ENABLED = "enabled";
    private static final String F_LAST_CARE_AT = "last_care_at";
    private static final String F_WEEK = "week";
    private static final String F_WEEK_COUNT = "week_count";
    private static final String F_IGNORED_STREAK = "ignored_streak";

    private final EmotionTimelineService timeline;
    private final ConversationService conversationService;
    private final SoulComfortService soulComfortService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${care.quiet-start:22:30}")
    private String quietStart;

    @Value("${care.quiet-end:08:00}")
    private String quietEnd;

    @Value("${care.daily-limit:1}")
    private int dailyLimit;

    @Value("${care.weekly-limit:3}")
    private int weeklyLimit;

    @Value("${care.weekly-limit-reduced:1}")
    private int weeklyLimitReduced;

    @Value("${care.window-days:14}")
    private int windowDays;

    public ProactiveCareService(EmotionTimelineService timeline,
                                ConversationService conversationService,
                                SoulComfortService soulComfortService,
                                StringRedisTemplate redis,
                                ObjectMapper objectMapper) {
        this.timeline = timeline;
        this.conversationService = conversationService;
        this.soulComfortService = soulComfortService;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // ===== 前端总入口：GET /care/pending =====

    /** 趋势（供晴雨云头像用）+ 待展示关怀卡（未生成或已过频控则为 null），命中缓存不重复生成 */
    public Map<String, Object> pending(String userId) {
        CareTrendEngine.CareTrend trend = CareTrendEngine.evaluate(timeline.recent(userId, windowDays));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", trend.name());
        result.put("card", readPendingCard(userId));
        return result;
    }

    /** 关怀卡是"应到未到再生成"：无缓存卡且时机合适时生成一张并暂存 */
    public Map<String, Object> pendingOrGenerate(String userId) {
        CareTrendEngine.CareTrend trend = CareTrendEngine.evaluate(timeline.recent(userId, windowDays));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", trend.name());
        CareCard card = readPendingCardObject(userId);
        if (card == null) {
            card = tryGenerate(userId, trend);
        }
        result.put("card", card != null ? cardToMap(card) : null);
        return result;
    }

    // ===== 开关：GET /care/settings / PUT /care/switch =====

    public Map<String, Object> settings(String userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", isEnabled(userId));
        return result;
    }

    public void setEnabled(String userId, boolean enabled) {
        stateOps(userId, F_ENABLED, String.valueOf(enabled));
        if (!enabled) {
            redis.delete(RedisConstants.carePendingKey(userId));
        }
    }

    // ===== 用户响应关怀卡：POST /care/{careId}/ack =====

    public void ack(String userId, String careId, String action) {
        CareCard cached = readPendingCardObject(userId);
        if (cached != null && cached.careId().equals(careId)) {
            redis.delete(RedisConstants.carePendingKey(userId));
        }
        if ("dismissed".equals(action)) {
            long streak = readStateLong(userId, F_IGNORED_STREAK, 0) + 1;
            stateOps(userId, F_IGNORED_STREAK, String.valueOf(streak));
        } else {
            stateOps(userId, F_IGNORED_STREAK, "0");
        }
    }

    // ===== 会话内"甜弈先开口"：POST /care/opener =====

    /**
     * 把关怀语作为 kind=care 的助手消息写入会话（自然进入模型记忆）。
     * refDate 非空时走解忧纸星路径：只引用该日的日期+心情词；
     * convId 为空时取最近会话，没有则新建一条"新对话"。
     */
    public Map<String, Object> opener(String userId, String convId, String refDate) {
        String text;
        if (refDate != null && !refDate.isBlank()) {
            text = starText(userId, LocalDate.parse(refDate));
        } else {
            CareCard card = readPendingCardObject(userId);
            text = card != null ? card.text() : null;
            if (text == null) {
                CareTrendEngine.CareTrend trend = CareTrendEngine.evaluate(timeline.recent(userId, windowDays));
                text = trendText(userId, trend);
            }
        }

        String targetConvId = (convId != null && !convId.isBlank()) ? convId : latestConvId(userId);
        ChatMessageVO message = conversationService.appendCareMessage(targetConvId, userId, text);

        redis.delete(RedisConstants.carePendingKey(userId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("convId", targetConvId);
        result.put("message", message);
        return result;
    }

    // ===== 解忧纸星 / 情绪年轮数据源：GET /care/mood-calendar =====

    public List<Map<String, Object>> moodCalendar(String userId, int year) {
        List<EmotionPoint> points = timeline.moodCalendar(userId, year);
        return points.stream()
                .map(p -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("date", p.date());
                    item.put("emotion", p.emotion());
                    item.put("emotionWord", EMOTION_WORDS.getOrDefault(p.emotion(), p.emotion()));
                    return item;
                })
                .toList();
    }

    // ===== 内部：卡片生成与时机闸门 =====

    private CareCard tryGenerate(String userId, CareTrendEngine.CareTrend trend) {
        if (!isEnabled(userId) || trend == CareTrendEngine.CareTrend.NONE) {
            return null;
        }
        if (inQuietHours() || !frequencyAllows(userId)) {
            return null;
        }

        CareCard card = anniversaryCard(userId);
        if (card == null && (trend == CareTrendEngine.CareTrend.TREND_DOWN
                || trend == CareTrendEngine.CareTrend.PERSISTENT_LOW
                || trend == CareTrendEngine.CareTrend.RECOVERY)) {
            String text = trendText(userId, trend);
            card = new CareCard("cc_" + System.currentTimeMillis(), "trend", text, System.currentTimeMillis());
        }
        if (card == null) {
            return null;
        }

        writePendingCard(userId, card);
        recordCareSent(userId);
        return card;
    }

    /** 去年今日：同月同日、去年有打卡且情绪偏低 → 静态池关怀（不经过模型） */
    private CareCard anniversaryCard(String userId) {
        Optional<EmotionPoint> lastYear = timeline.onDate(userId, LocalDate.now().minusYears(1));
        if (lastYear.isEmpty() || !CareTrendEngine.isLow(lastYear.get().emotion())) {
            return null;
        }
        List<String> pool = ANNIVERSARY_POOLS.get(lastYear.get().emotion());
        if (pool == null) {
            return null;
        }
        String text = pool.get((int) (System.currentTimeMillis() % pool.size()));
        return new CareCard("cc_" + System.currentTimeMillis(), "anniversary", text, System.currentTimeMillis());
    }

    /** 趋势关怀语：模型生成 → 校验 → 兜底池 */
    private String trendText(String userId, CareTrendEngine.CareTrend trend) {
        if (trend == CareTrendEngine.CareTrend.NONE) {
            return "最近还好吗？我在这里，随时可以聊聊。";
        }
        String text = null;
        try {
            String response = soulComfortService.chatForReport(buildTrendPrompt(userId, trend));
            text = sanitize(response);
        } catch (Exception e) {
            log.warn("[主动关怀] 模型生成失败 userId={} trend={}: {}", userId, trend, e.getMessage());
        }
        if (text == null) {
            List<String> pool = FALLBACK_POOLS.get(trend.name());
            text = pool != null ? pool.get((int) (System.currentTimeMillis() % pool.size()))
                    : "我在呢，愿意的话，跟我说说最近的日子。";
        }
        return text;
    }

    /** 解忧纸星：点开某颗"难过星"时生成的回访语，输入只有日期+心情词 */
    private String starText(String userId, LocalDate refDate) {
        Optional<EmotionPoint> point = timeline.onDate(userId, refDate);
        String word = point.map(p -> EMOTION_WORDS.getOrDefault(p.emotion(), p.emotion()))
                .orElse("不太开心");
        String text = null;
        try {
            String response = soulComfortService.chatForReport(
                    "你是甜弈。用户在 " + refDate + " 签到时的心情是「" + word + "」，今天他点开了这一天。"
                            + "请写一句 40 字以内的回访：温柔、只面向当下，可以说\"那天\"，"
                            + "但严禁引用或猜测他当时的原话，不要给建议清单。只输出这句话。");
            text = sanitize(response);
        } catch (Exception e) {
            log.warn("[主动关怀] 纸星文案生成失败 refDate={}: {}", refDate, e.getMessage());
        }
        if (text == null) {
            text = "看到你在那天选择了「" + word + "」，如果想和现在的自己聊聊那天，我都在。";
        }
        return text;
    }

    private String buildTrendPrompt(String userId, CareTrendEngine.CareTrend trend) {
        List<EmotionPoint> points = timeline.recent(userId, windowDays);
        StringBuilder seq = new StringBuilder();
        for (EmotionPoint p : points) {
            if (seq.length() > 0) {
                seq.append("、");
            }
            seq.append(p.date().substring(5)).append(" ").append(EMOTION_WORDS.getOrDefault(p.emotion(), p.emotion()));
        }
        String scene = switch (trend) {
            case TREND_DOWN -> "最近心情有走低的趋势";
            case PERSISTENT_LOW -> "连续几天心情都比较低";
            case RECOVERY -> "心情正在从低谷回暖";
            default -> "想了解近况";
        };
        return "你是甜弈。用户" + scene + "，他近两周的签到心情序列（只有日期和心情词，没有任何原话）："
                + seq + "。请写一句 50 字以内的主动关怀："
                + "情绪优先、陪伴感、可以轻轻提及\"最近\"，"
                + "严禁引用或猜测用户说过的话、严禁建议清单（如首先/其次/你可以尝试）、不要提问超过一个。只输出这句话。";
    }

    /** 关怀文案校验：剥标签、去身份泄漏、拒引用原话/清单体、限长；不合格返回 null 交给兜底池 */
    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = SONG_TAG.matcher(raw).replaceAll("");
        text = EMOTION_TAG.matcher(text).replaceAll("");
        text = IdentityLeakScrubber.scrub(text).replaceAll("\\s+", " ").trim();
        if (text.isEmpty() || text.length() > 160) {
            return null;
        }
        if (QUOTE_PATTERNS.matcher(text).find() || NUMBERED_LIST.matcher(text).find()) {
            log.info("[主动关怀] 文案越界已弃用: {}", text);
            return null;
        }
        return text;
    }

    // ===== 内部：Redis 状态与缓存 =====

    private boolean isEnabled(String userId) {
        String v = readState(userId, F_ENABLED);
        return v == null || "true".equals(v);
    }

    private boolean inQuietHours() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(quietStart);
        LocalTime end = LocalTime.parse(quietEnd);
        // 22:30-24:00 或 00:00-08:00 静默
        return !now.isBefore(start) || now.isBefore(end);
    }

    private boolean frequencyAllows(String userId) {
        long last = readStateLong(userId, F_LAST_CARE_AT, 0);
        if (last > 0 && LocalDate.ofInstant(java.time.Instant.ofEpochMilli(last), java.time.ZoneId.systemDefault())
                .isEqual(LocalDate.now())) {
            return false;
        }
        String week = currentWeekKey();
        String storedWeek = readState(userId, F_WEEK);
        int sentThisWeek = week.equals(storedWeek) ? (int) readStateLong(userId, F_WEEK_COUNT, 0) : 0;
        long streak = readStateLong(userId, F_IGNORED_STREAK, 0);
        int limit = streak >= 2 ? weeklyLimitReduced : weeklyLimit;
        return sentThisWeek < limit;
    }

    private void recordCareSent(String userId) {
        stateOps(userId, F_LAST_CARE_AT, String.valueOf(System.currentTimeMillis()));
        String week = currentWeekKey();
        String storedWeek = readState(userId, F_WEEK);
        int sentThisWeek = week.equals(storedWeek) ? (int) readStateLong(userId, F_WEEK_COUNT, 0) : 0;
        stateOps(userId, F_WEEK, week);
        stateOps(userId, F_WEEK_COUNT, String.valueOf(sentThisWeek + 1));
    }

    private String currentWeekKey() {
        LocalDate today = LocalDate.now();
        return today.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
                + "-W" + today.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    private String readState(String userId, String field) {
        Object v = redis.opsForHash().get(RedisConstants.careStateKey(userId), field);
        return v == null ? null : String.valueOf(v);
    }

    private long readStateLong(String userId, String field, long fallback) {
        String v = readState(userId, field);
        if (v == null) {
            return fallback;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void stateOps(String userId, String field, String value) {
        String key = RedisConstants.careStateKey(userId);
        redis.opsForHash().put(key, field, value);
        redis.expire(key, RedisConstants.DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    private void writePendingCard(String userId, CareCard card) {
        try {
            redis.opsForValue().set(RedisConstants.carePendingKey(userId),
                    objectMapper.writeValueAsString(card), 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("[主动关怀] 关怀卡缓存写入失败 userId={}: {}", userId, e.getMessage());
        }
    }

    private Map<String, Object> readPendingCard(String userId) {
        CareCard card = readPendingCardObject(userId);
        return card == null ? null : cardToMap(card);
    }

    private CareCard readPendingCardObject(String userId) {
        String json = redis.opsForValue().get(RedisConstants.carePendingKey(userId));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CareCard.class);
        } catch (Exception e) {
            log.warn("[主动关怀] 关怀卡缓存解析失败: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> cardToMap(CareCard card) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("careId", card.careId());
        map.put("type", card.type());
        map.put("text", card.text());
        map.put("createdAt", card.createdAt());
        return map;
    }

    private String latestConvId(String userId) {
        var page = conversationService.list(userId, 1, 1);
        if (page.getRecords() != null && !page.getRecords().isEmpty()) {
            return page.getRecords().get(0).getId();
        }
        return conversationService.create(userId, "新对话", null).getId();
    }
}
