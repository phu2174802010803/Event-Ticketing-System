package com.example.eventservice.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Float>> vertices;

    @Column
    private String zone;

    @Column(name = "fill_color")
    private String fillColor;

    @Column(name = "is_stage", nullable = false, columnDefinition = "boolean default false")
    private boolean isStage; // Thuộc tính mới
}