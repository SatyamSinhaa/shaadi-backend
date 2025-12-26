package com.shaadi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {
    private Long subscriptionId;
    private Long userId;
    private Long planId;
    private String planName;
    private Integer planDurationMonths;
    private Integer planChatLimit;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private String status;
    private Integer chatLimit;
}
