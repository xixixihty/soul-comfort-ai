package com.hxq.soulcomfortai.dto.request;

import lombok.Data;

@Data
public class UpdateDiaryRequest {

    private String title;
    private String content;
    private String mood;
}