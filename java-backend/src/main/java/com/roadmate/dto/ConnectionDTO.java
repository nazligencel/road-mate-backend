package com.roadmate.dto;

import com.roadmate.model.Connection.ConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionDTO {
    private Long id;
    private UserDTO user;
    private UserDTO connectedUser;
    private ConnectionStatus status;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDTO {
        private Long id;
        private String name;
        private String email;
        private String image;
        private String vehicle;
        private String vehicleModel;
        private String status;
        private String route;
        private String profileImageUrl;
    }
}
