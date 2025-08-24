package com.example.eventservice.dto;

import lombok.Data;

@Data
public class PhaseUpdateEvent {
    private Integer eventId;
    private Integer phaseId;
    private Integer availableTickets;
}