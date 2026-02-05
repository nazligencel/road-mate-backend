package com.roadmate.dto;

import lombok.Data;

@Data
public class CreateActivityRequest {
    private String title;
    private String description;
    private String location;
    private String date;
    private String time;
    private String type;
    private String image;
}
