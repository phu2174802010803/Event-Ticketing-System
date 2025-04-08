package com.example.eventservice.dto;

import lombok.Data;

@Data
public class TemplateAreaResponseDto {
    private Integer templateAreaId;
    private String name;

    public TemplateAreaResponseDto(Integer templateAreaId, String name) {
        this.templateAreaId = templateAreaId;
        this.name = name;
    }
}