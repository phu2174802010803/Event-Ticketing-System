package com.example.eventservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SellingPhaseResponseDto {
    private Integer phaseId;
    private Integer eventId;
    private Integer areaId;
    private String areaName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer ticketsAvailable;
    private String status;
    private String message;

    public SellingPhaseResponseDto(Integer phaseId, Integer eventId, Integer areaId, String areaName, LocalDateTime startTime,
                                   LocalDateTime endTime, Integer ticketsAvailable, String status, String message) {
        this.phaseId = phaseId;
        this.eventId = eventId;
        this.areaId = areaId;
        this.areaName = areaName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.ticketsAvailable = ticketsAvailable;
        this.status = status;
        this.message = message;
    }
}