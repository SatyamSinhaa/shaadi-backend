package com.shaadi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String email;
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    // Profile fields (nullable for admins)
    private Integer age;
    private String gender;
    private String religion;
    private String location;
    private String bio;
    private String photoUrl;

    // Freebie chat limit for new users
    @Column(name = "free_chat_limit", nullable = false)
    private Integer freeChatLimit = 2; // Default 2, configurable by admin

    // Subscription relationship
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Subscription> subscriptions;
}
