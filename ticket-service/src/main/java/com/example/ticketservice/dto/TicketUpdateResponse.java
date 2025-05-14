package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketUpdateResponse {
    private Integer areaId;
    private String availableTickets;
    private String message;

    public TicketUpdateResponse(Integer areaId, String availableTickets, String message) {
        this.areaId = areaId;
        this.availableTickets = availableTickets;
        this.message = message;
    }
}
