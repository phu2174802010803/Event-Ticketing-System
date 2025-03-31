package com.example.eventservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "areas")
@Data
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "event_id", nullable = false)
    private Integer eventId;

    @Column(name = "template_area_id")
    private Integer templateAreaId;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;

    @Column(name = "available_tickets", nullable = false)
    private Integer availableTickets;

    @Column(nullable = false)
    private Double price;
}