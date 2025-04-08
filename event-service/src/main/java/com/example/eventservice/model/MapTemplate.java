package com.example.eventservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "map_templates")
@Data
public class MapTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Integer templateId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "area_count", nullable = false)
    private Integer areaCount;

    @OneToMany(mappedBy = "mapTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateArea> areas;
}