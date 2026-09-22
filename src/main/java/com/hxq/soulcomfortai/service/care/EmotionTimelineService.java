package com.hxq.soulcomfortai.service.care;

import com.hxq.soulcomfortai.entity.EmotionCheckin;
import com.hxq.soulcomfortai.repository.EmotionCheckinRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 情绪时间线只读投影：主动关怀功能唯一的情绪数据入口。
 * <p>
 * 隐私红线的代码级落点——本服务是打卡数据与关怀引擎之间的漏斗：
 * 对外只吐 {date, emotion}，note 字段在这里被物理丢弃，
 * 关怀文案的模型输入因此永远接触不到用户写下的原话。
 * 不新建 Redis 投影键：直接以打卡仓储为源计算，老用户零回填、无双写一致性问题。
 */
@Slf4j
@Service
public class EmotionTimelineService {

    /** 时间线上的一天：只有日期和心情词 */
    public record EmotionPoint(String date, String emotion) {}

    private final EmotionCheckinRepository checkinRepository;

    public EmotionTimelineService(EmotionCheckinRepository checkinRepository) {
        this.checkinRepository = checkinRepository;
    }

    /** 最近 days 天的情绪序列（按时间升序） */
    public List<EmotionPoint> recent(String userId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        return toPoints(checkinRepository.findByDateRange(userId, start.toString(), end.toString()));
    }

    /** 某年全部打卡的心情点阵（情绪年轮/解忧纸星的数据源，不含 note） */
    public List<EmotionPoint> moodCalendar(String userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return toPoints(checkinRepository.findByDateRange(userId, start.toString(), end.toString()));
    }

    /** 指定日期的心情（去年今日的年轮回溯用），未打卡返回空 */
    public Optional<EmotionPoint> onDate(String userId, LocalDate date) {
        EmotionCheckin checkin = checkinRepository.findByDate(userId, date.toString());
        return checkin == null ? Optional.empty()
                : Optional.of(new EmotionPoint(checkin.getDate(), checkin.getEmotion()));
    }

    /** 暖池：lookbackDays 内最近一次正面心情（开心/感恩/活力），供关怀语轻量引用日期+心情词 */
    public Optional<EmotionPoint> latestPositive(String userId, int lookbackDays) {
        List<EmotionPoint> points = recent(userId, lookbackDays);
        for (int i = points.size() - 1; i >= 0; i--) {
            if (CareTrendEngine.isPositive(points.get(i).emotion())) {
                return Optional.of(points.get(i));
            }
        }
        return Optional.empty();
    }

    private List<EmotionPoint> toPoints(List<EmotionCheckin> checkins) {
        List<EmotionPoint> points = new ArrayList<>(checkins.size());
        for (EmotionCheckin c : checkins) {
            points.add(new EmotionPoint(c.getDate(), c.getEmotion()));
        }
        return points;
    }
}
