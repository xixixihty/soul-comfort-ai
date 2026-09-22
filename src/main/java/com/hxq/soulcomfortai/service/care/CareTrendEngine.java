package com.hxq.soulcomfortai.service.care;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 情绪趋势引擎：把最近的情绪序列折叠成一个"该不该开口"的判断。
 * <p>
 * 原则是宁可不说、不可说错——样本不足 3 次打卡一律 NONE（不打扰）。
 * 情绪分级沿用打卡的 8 个枚举：0=晴（开心/感恩/活力），1=多云（平静），
 * 2=小雨（疲惫），3=大雨（难过/焦虑/生气）。
 */
@Service
public class CareTrendEngine {

    public enum CareTrend { NONE, TREND_DOWN, PERSISTENT_LOW, RECOVERY }

    private static final Map<String, Integer> SEVERITY = Map.of(
            "happy", 0, "grateful", 0, "energetic", 0,
            "calm", 1,
            "tired", 2,
            "sad", 3, "anxious", 3, "angry", 3);

    /** 未识别的情绪词按"多云"处理，不触发关怀 */
    public static int severity(String emotion) {
        return SEVERITY.getOrDefault(emotion, 1);
    }

    public static boolean isLow(String emotion) {
        return severity(emotion) >= 2;
    }

    public static boolean isPositive(String emotion) {
        return severity(emotion) == 0;
    }

    /**
     * points 按时间升序。判定顺序：持续低压 > 下滑 > 回暖 > 无。
     * - PERSISTENT_LOW：最近 3 次打卡全在雨里（severity>=2）；
     * - TREND_DOWN：severity 逐级变阴且当前在雨里；
     * - RECOVERY：此前在雨里、最近 2 次转晴/多云（供头像回暖态与未来的"晴天明信片"用）。
     */
    public static CareTrend evaluate(List<EmotionTimelineService.EmotionPoint> points) {
        if (points == null || points.size() < 3) {
            return CareTrend.NONE;
        }
        int n = points.size();
        int a = severity(points.get(n - 3).emotion());
        int b = severity(points.get(n - 2).emotion());
        int c = severity(points.get(n - 1).emotion());

        if (a >= 2 && b >= 2 && c >= 2) {
            return CareTrend.PERSISTENT_LOW;
        }
        if (c >= 2 && c >= b && b >= a && c > a) {
            return CareTrend.TREND_DOWN;
        }
        if (c <= 1 && b <= 1) {
            for (int i = 0; i < n - 2; i++) {
                if (severity(points.get(i).emotion()) >= 2) {
                    return CareTrend.RECOVERY;
                }
            }
        }
        return CareTrend.NONE;
    }
}
