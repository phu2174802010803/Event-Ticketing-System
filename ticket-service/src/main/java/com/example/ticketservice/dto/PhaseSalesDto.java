package com.example.ticketservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhaseSalesDto {
    private Integer phaseId;
    private Integer areaId;
    private String areaName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer soldTickets;
    private Integer availableTickets;
    private Double revenue;
    private String status; // upcoming, active, ended
}