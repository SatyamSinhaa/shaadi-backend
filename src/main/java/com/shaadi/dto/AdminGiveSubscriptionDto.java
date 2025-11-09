package com.shaadi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminGiveSubscriptionDto {
    private Integer userId;
    private Integer planId;
    private Integer durationMonths; // Optional, defaults to plan's duration
}
