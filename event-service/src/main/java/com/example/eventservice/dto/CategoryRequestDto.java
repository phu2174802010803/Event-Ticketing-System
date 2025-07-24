package com.example.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequestDto {
    @NotBlank(message = "Tên danh mục là bắt buộc")
    private String name;

    private String description;
}