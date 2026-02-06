package com.roadmate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NomadDto {
    private Long id;
    private String name;
    private String image;
    private String status;
    private String vehicle;
    private String vehicleModel;
    private String route;
    private Double latitude;
    private Double longitude;
    private Double distance;
    private Boolean online;
    private Boolean sosActive;
    private Boolean showRoute;

    // Coordinate object for frontend compatibility
    private Coordinate coordinate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinate {
        private Double latitude;
        private Double longitude;
    }
}
