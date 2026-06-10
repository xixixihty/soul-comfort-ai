package com.hxq.soulcomfortai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Diary {

    private String id;
    private String userId;
    private String title;
    private String content;
    private String mood;
    private String aiResonance;
    private long resonanceAt;
    private long createdAt;
    private long updatedAt;
}