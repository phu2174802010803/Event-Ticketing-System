package com.example.eventservice.dto;

import lombok.Data;

@Data
public class CategoryPublicListDto {
    private Integer categoryId;
    private String name;
    private String description;
}
