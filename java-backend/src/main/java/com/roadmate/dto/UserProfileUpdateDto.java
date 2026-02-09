package com.roadmate.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateDto {
    @Size(max = 50, message = "Name must be at most 50 characters")
    private String name;

    @Size(max = 100, message = "Vehicle must be at most 100 characters")
    private String vehicle;

    @Size(max = 100, message = "Vehicle brand must be at most 100 characters")
    private String vehicleBrand;

    @Size(max = 100, message = "Vehicle model must be at most 100 characters")
    private String vehicleModel;

    @Size(max = 100, message = "Status must be at most 100 characters")
    private String status;

    @Size(max = 50, message = "Tagline must be at most 50 characters")
    private String tagline;

    @Size(max = 100, message = "Location must be at most 100 characters")
    private String location;
}
