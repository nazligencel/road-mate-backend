package com.roadmate.dto;

import lombok.Data;

@Data
public class CreateDiscussionRequest {
    private String title;
    private String description;
    private String tag;
    private String image;
}
