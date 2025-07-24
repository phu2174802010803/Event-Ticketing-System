package com.example.eventservice.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Data
public class SellingPhaseRequestDto {
    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    private LocalDateTime endTime;

    @NotNull(message = "Số vé khả dụng là bắt buộc")
    private Integer ticketsAvailable;

    private Integer areaId;

    private String areaName;
}