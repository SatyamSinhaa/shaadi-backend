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
    private Long id;

    private String name; // e.g., "Basic 1 Month", "Premium 3 Months"
    private Integer durationMonths; // e.g., 1 or 3
    private Double price; // e.g., 10.0
    private Boolean isPublished = false; // Admin controls visibility
    private Boolean isAddon = false; // Indicates if this is an addon plan for chat limit extension
    private Integer chatLimit; // Max number of unique users the subscriber can chat with, e.g., 10
}
