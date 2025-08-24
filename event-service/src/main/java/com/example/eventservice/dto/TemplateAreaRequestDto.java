package com.example.eventservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

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

    private List<Map<String, Float>> vertices; // Danh sách tọa độ [{x: float, y: float}, ...]
    private String zone;                       // Tên vùng (ví dụ: "ZONE A")
    private String fillColor;                  // Màu sắc (ví dụ: "#000000")

    @JsonProperty("isStage")
    private boolean isStage;
}