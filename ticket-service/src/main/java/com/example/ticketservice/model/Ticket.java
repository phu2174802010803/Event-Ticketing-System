package com.example.ticketservice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Column(name = "event_id", nullable = false)
    private Integer eventId;

    @Column(name = "area_id", nullable = false)
    private Integer areaId;

    @Column(name = "phase_id")
    private Integer phaseId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "ticket_code", unique = true, nullable = false)
    private String ticketCode;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "area_name", nullable = false)
    private String areaName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}