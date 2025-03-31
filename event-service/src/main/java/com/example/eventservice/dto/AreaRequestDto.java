package com.example.eventservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AreaRequestDto {
    @NotBlank(message = "Tên khu vực là bắt buộc")
    private String name;

    private Integer templateAreaId;

    @NotNull(message = "Tổng số vé là bắt buộc")
    private Integer totalTickets;

    @NotNull(message = "Giá vé là bắt buộc")
    private Double price;
}
