package com.example.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateAreaRequestDto {
    @NotBlank(message = "Tên khu vực là bắt buộc")
    private String name;
}