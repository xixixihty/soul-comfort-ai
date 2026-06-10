package com.hxq.soulcomfortai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionRecord {

    private String id;
    private String userId;
    private String convId;
    private String messageId;
    private String emotion;
    private String date;
    private long timestamp;
}