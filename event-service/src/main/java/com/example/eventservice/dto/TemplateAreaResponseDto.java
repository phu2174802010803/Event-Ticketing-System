package com.example.eventservice.dto;

import lombok.Data;

@Data
public class TemplateAreaResponseDto {
    private Integer templateAreaId;
    private String name;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;

    public TemplateAreaResponseDto(Integer templateAreaId, String name, Integer x, Integer y, Integer width, Integer height) {
        this.templateAreaId = templateAreaId;
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}