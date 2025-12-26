package com.shaadi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminGiveSubscriptionDto {
    private Long userId;
    private Long planId;
    private Integer durationMonths; // Optional, defaults to plan's duration
}
