package com.shaadi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name; // e.g., "Basic 1 Month", "Premium 3 Months"
    private Integer durationMonths; // e.g., 1 or 3
    private Double price; // e.g., 10.0
    private Boolean isPublished = false; // Admin controls visibility
    private Integer chatLimit; // Max number of unique users the subscriber can chat with, e.g., 10
}
