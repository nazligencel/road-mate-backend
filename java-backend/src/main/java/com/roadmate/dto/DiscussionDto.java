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
public class DiscussionDto {
    private Long id;
    private String title;
    private String description;
    private String tag;
    private String image;
    private LocalDateTime createdAt;

    // Creator info
    private Long creatorId;
    private String creatorName;
    private String creatorImage;

    // Computed
    private Integer commentCount;
    private Boolean isSaved;
    private String timeAgo;
}
