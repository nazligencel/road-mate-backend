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
public class DiscussionCommentDto {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorImage;
    private String text;
    private LocalDateTime createdAt;
    private String timeAgo;
}
