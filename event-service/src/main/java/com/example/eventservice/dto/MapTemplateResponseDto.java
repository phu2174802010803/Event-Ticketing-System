package com.example.eventservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class MapTemplateResponseDto {
    private Integer templateId;
    private String name;
    private String description;
    private Integer areaCount;
    private Integer mapWidth;
    private Integer mapHeight;
    private List<TemplateAreaResponseDto> areas;
    private String message;

    public MapTemplateResponseDto(Integer templateId, String name, String description, Integer areaCount,
                                  Integer mapWidth, Integer mapHeight, List<TemplateAreaResponseDto> areas, String message) {
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.areaCount = areaCount;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.areas = areas;
        this.message = message;
    }
}