package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class EventSalesSummaryDto {
    private Integer eventId;
    private String eventName;
    private Integer soldTickets;
    private Double totalRevenue;
}