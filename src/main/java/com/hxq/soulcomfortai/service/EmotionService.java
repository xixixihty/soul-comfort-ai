package com.hxq.soulcomfortai.service;

import com.hxq.soulcomfortai.entity.EmotionRecord;
import com.hxq.soulcomfortai.repository.EmotionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EmotionService {

    private static final Pattern EMOTION_PATTERN = Pattern.compile("【情绪】(\\w+)\\s*【回复】(.*)", Pattern.DOTALL);

    private final EmotionRepository emotionRepository;

    public EmotionService(EmotionRepository emotionRepository) {
        this.emotionRepository = emotionRepository;
    }

    public void tryRecordEmotion(String userId, String convId, String rawResponse) {
        Matcher matcher = EMOTION_PATTERN.matcher(rawResponse);
        if (!matcher.find()) {
            log.debug("未从回复中解析到情绪标签 convId={}", convId);
            return;
        }

        String emotion = matcher.group(1).trim();
        String date = LocalDate.now().toString();
        String id = emotionRepository.nextId();

        EmotionRecord record = EmotionRecord.builder()
                .id(id)
                .userId(userId)
                .convId(convId)
                .emotion(emotion)
                .date(date)
                .timestamp(System.currentTimeMillis())
                .build();

        emotionRepository.save(record);
        log.debug("情绪记录已保存 emotionId={} emotion={}", id, emotion);
    }
}