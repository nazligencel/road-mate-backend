package com.roadmate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderImage;
    private Long receiverId;
    private String receiverName;
    private String receiverImage;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Boolean isMine; // Helper field for frontend
}
