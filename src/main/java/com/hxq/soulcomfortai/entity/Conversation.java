package com.hxq.soulcomfortai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    private String id;
    private String userId;
    private String title;
    private String tag;
    private long createdAt;
    private long updatedAt;
}