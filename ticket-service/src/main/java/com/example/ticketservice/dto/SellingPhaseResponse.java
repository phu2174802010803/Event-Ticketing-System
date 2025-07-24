package com.example.ticketservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SellingPhaseResponse {
    private Integer phaseId;
    private Integer eventId;
    private Integer areaId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer ticketsAvailable;
    private String message;
}