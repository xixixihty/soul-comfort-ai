package com.hxq.soulcomfortai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    private String id;
    private String role;
    private String content;
    private String emotion;
    /** 消息种类：null=普通聊天，"care"=甜弈主动关怀（先开口/拆星寄语） */
    private String kind;
    private long timestamp;
    private boolean revoked;
    @Builder.Default
    private List<EditRecord> editHistory = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditRecord {
        private String content;
        private long editedAt;
    }
}