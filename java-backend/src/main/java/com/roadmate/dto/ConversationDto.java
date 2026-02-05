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
public class ConversationDto {
    private Long odUserId; // Other user's ID
    private String otherUserName;
    private String otherUserImage;
    private Boolean otherUserOnline;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Long unreadCount;
}
