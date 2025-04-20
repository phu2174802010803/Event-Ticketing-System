package com.example.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateAreaRequestDto {
    @NotBlank(message = "Tên khu vực là bắt buộc")
    private String name;

    @NotNull(message = "Tọa độ X là bắt buộc")
    private Integer x;

    @NotNull(message = "Tọa độ Y là bắt buộc")
    private Integer y;

    @NotNull(message = "Chiều rộng là bắt buộc")
    private Integer width;

    @NotNull(message = "Chiều cao là bắt buộc")
    private Integer height;
}