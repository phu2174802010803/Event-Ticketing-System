package com.example.ticketservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PhaseSalesDto {
    private Integer phaseId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer soldTickets;
    private Double revenue;
}