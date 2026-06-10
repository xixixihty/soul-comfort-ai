package com.hxq.soulcomfortai.dto.response;

import com.hxq.soulcomfortai.entity.ChatMessageVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {

    private String id;
    private String userId;
    private String title;
    private String tag;
    private long createdAt;
    private long updatedAt;
    private List<ChatMessageVO> messages;
}