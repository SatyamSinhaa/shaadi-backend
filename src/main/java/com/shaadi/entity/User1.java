package com.shaadi.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User1 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
//    personal
    @Column(nullable = false)
    private String firstName;
    private String middleName; // optional
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = true)
    private String caste;
    @Column(nullable = true)
    private String gotr;
    @Column(nullable = true)
    private LocalDate dateOfBirth;
    @Column(nullable = true)
    private Double heightFeet;
    @Column(nullable = true)
    private Double weight;
    @Column(nullable = true)
    private String education;
    @Column(nullable = true)
    private String occupation;
    @Column(nullable = true)
    private String income;
    
//    family
    @Column(nullable = true)
    private String fatherName;
    @Column(nullable = true)
    private String fatherOccupation;
    @Column(nullable = true)
    private String motherName;
    @Column(nullable = true)
    private String motherOccupation;
    @Column(nullable = true)
    private int brothersCount;
    @Column(nullable = true)
    private String brothersNames;
    @Column(nullable = true)
    private int sistersCount;
    @Column(nullable = true)
    private String sistersNames;
    
//    others
    @Column(nullable = true)
    private String maternalStatus;
    @Column(nullable = true)
    private String property;
    @Column(nullable = true)
    private String address;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(nullable = true)
    private String contactNumber;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;
}
