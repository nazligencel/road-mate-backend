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
public class ActivityDto {
    private Long id;
    private String title;
    private String description;
    private String location;
    private String date;
    private String time;
    private String type;
    private String image;
    private String status;
    private LocalDateTime createdAt;

    // Creator info
    private Long creatorId;
    private String creatorName;
    private String creatorImage;

    // Participant count
    private Integer participantCount;
    private Boolean hasJoined; // Whether current user has joined
}
