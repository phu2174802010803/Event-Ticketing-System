package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class PhaseUpdateEvent {
    private Integer eventId;
    private Integer phaseId;
    private Integer availableTickets;
}