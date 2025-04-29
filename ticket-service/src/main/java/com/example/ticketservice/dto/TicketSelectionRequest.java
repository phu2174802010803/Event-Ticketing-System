package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketSelectionRequest {
    private Integer eventId;
    private Integer areaId;
    private Integer quantity;
}