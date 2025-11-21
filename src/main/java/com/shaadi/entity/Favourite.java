package com.shaadi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Favourite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The user who favourited

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favourited_user_id", nullable = false)
    private User favouritedUser; // The user being favourited
}
