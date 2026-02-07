package com.roadmate.dto;

import lombok.Data;

@Data
public class UserProfileUpdateDto {
    private String name;
    private String vehicle;
    private String vehicleBrand;
    private String vehicleModel;
    private String status;
    private String tagline;
    private String location;
}
