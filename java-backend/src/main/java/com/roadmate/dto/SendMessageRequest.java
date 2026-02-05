package com.roadmate.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long receiverId;
    private String content;
}
