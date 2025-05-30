package com.example.ticketservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventSalesResponseDto {
    private Integer eventId;
    private String eventName;
    private Integer totalTickets;
    private Integer soldTickets;
    private Integer availableTickets;
    private Double totalRevenue;
    private List<AreaSalesDto> areas;
}