package com.example.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AreaRequestDto {
    @NotBlank(message = "Tên khu vực là bắt buộc")
    private String name;

    private Integer templateAreaId;

    private Integer totalTickets; // Không bắt buộc khi isStage = true
    private Double price;         // Không bắt buộc khi isStage = true
}
