package com.example.ticketservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class SystemSalesResponseDto {
    private Integer totalEvents;
    private Integer totalSoldTickets;
    private Double totalRevenue;
    private List<EventSalesSummaryDto> events;
    private Integer page;
    private Integer size;
}