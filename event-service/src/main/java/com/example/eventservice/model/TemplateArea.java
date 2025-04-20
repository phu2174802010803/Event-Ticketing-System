package com.example.eventservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "template_areas")
@Data
public class TemplateArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_area_id")
    private Integer templateAreaId;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private MapTemplate mapTemplate;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer x;

    @Column(nullable = false)
    private Integer y;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;
}