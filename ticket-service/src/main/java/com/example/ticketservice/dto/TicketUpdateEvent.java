package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketUpdateEvent {
    private Integer eventId;
    private Integer areaId;
    private Integer availableTickets;
}