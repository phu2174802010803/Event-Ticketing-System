package com.example.eventservice.dto;

import lombok.Data;

@Data
public class CategoryResponseDto {
    private Integer categoryId;
    private String name;
    private String description;
    private String message;

    public CategoryResponseDto(Integer categoryId, String name, String description, String message) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.message = message;
    }
}