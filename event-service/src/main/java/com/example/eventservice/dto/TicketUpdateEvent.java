package com.example.eventservice.dto;

import lombok.Data;

@Data
public class TicketUpdateEvent {
    private Integer eventId;
    private Integer areaId;
    private Integer availableTickets;
}