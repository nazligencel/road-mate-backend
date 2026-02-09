package com.roadmate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    @NotBlank(message = "Message content is required")
    @Size(max = 5000, message = "Message must be at most 5000 characters")
    private String content;
}
