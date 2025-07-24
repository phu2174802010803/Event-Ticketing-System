package com.example.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MapTemplateRequestDto {
    @NotBlank(message = "Tên template là bắt buộc")
    private String name;

    private String description;

    @NotNull(message = "Số lượng khu vực là bắt buộc")
    private Integer areaCount;

    @NotNull(message = "Chiều rộng map là bắt buộc")
    private Integer mapWidth;

    @NotNull(message = "Chiều cao map là bắt buộc")
    private Integer mapHeight;

    private List<TemplateAreaRequestDto> areas;
}