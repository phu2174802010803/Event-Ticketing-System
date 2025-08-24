package com.example.eventservice.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TemplateAreaResponseDto {
    private Integer templateAreaId;
    private String name;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private List<Map<String, Float>> vertices; // Danh sách tọa độ
    private String zone;                       // Tên vùng
    private String fillColor; // Màu sắc
    private boolean isStage;

    public TemplateAreaResponseDto(Integer templateAreaId, String name, Integer x, Integer y, Integer width, Integer height,
                                   List<Map<String, Float>> vertices, String zone, String fillColor, boolean isStage) {
        this.templateAreaId = templateAreaId;
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.vertices = vertices;
        this.zone = zone;
        this.fillColor = fillColor;
        this.isStage = isStage;
    }
}