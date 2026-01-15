package com.shaadi.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    // @JsonIgnore
    // private String password;
    @Column(unique = true)
    private String firebaseUid;
    @Column(unique = true)
    @Size(min = 10, max = 10, message = "Mobile number must be exactly 10 digits")
    @Pattern(regexp = "\\d{10}", message = "Mobile number must contain only digits")
    private String mobileNumber;

    // Personal Details
    private Integer age;
    private String gender;
    private String maritalStatus;
    private Boolean manglik;
    private LocalDate dateOfBirth;
    private Integer height;
    private Integer weightKg;
    private String rashi;
    private String gotr;
    private String caste;
    private String category;
    private String religion;
    private String profession;
    private String education;
    private Long annualIncome;
    private String motherTongue;

    // Location
    private String cityTown;
    private String district;
    private String state;

    // Family Details
    private String fatherName;
    private String fatherOccupation;
    private String motherName;
    private String motherOccupation;
    private Integer numberOfBrothers;
    private Integer numberOfSisters;
    private String familyType;
    private String familyLocations;
    private String property;

    // Lifestyle
    private String diet;
    private Boolean smoking;
    private Boolean drinking;

    // Other Profile Fields
    private String bio;
    private String photoUrl;
    private String fcmToken;
    private Boolean isProfileComplete = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    // Photo gallery
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Photo> photos;


    // Subscription relationship
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Subscription> subscriptions;
}
