package com.hxq.soulcomfortai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionCheckin {

    private String id;
    private String userId;
    private String emotion;
    private String note;
    private String date;
    private long timestamp;
}