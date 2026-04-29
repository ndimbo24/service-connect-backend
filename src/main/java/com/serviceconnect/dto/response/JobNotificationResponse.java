package com.serviceconnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobNotificationResponse {
    private Long id;
    private Long requestId;
    private String serviceType;
    private String clientName;
    private Double distance;
    private String address;
    private String description;
    private LocalDateTime expiresAt;
    private String status;
}
