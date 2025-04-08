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

    private List<TemplateAreaRequestDto> areas;
}