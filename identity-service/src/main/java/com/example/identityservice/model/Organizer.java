package com.example.identityservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizers")
@Data
public class Organizer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "organizer_id")
    private Integer organizerId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}