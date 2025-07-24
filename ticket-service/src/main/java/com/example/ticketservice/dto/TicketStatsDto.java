package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketStatsDto {
    private long totalTickets;
    private long soldTickets;
    private long usedTickets;
    private long cancelledTickets;
    private double totalRevenue;

    public TicketStatsDto() {
    }

    public TicketStatsDto(long totalTickets, long soldTickets, long usedTickets,
                          long cancelledTickets, double totalRevenue) {
        this.totalTickets = totalTickets;
        this.soldTickets = soldTickets;
        this.usedTickets = usedTickets;
        this.cancelledTickets = cancelledTickets;
        this.totalRevenue = totalRevenue;
    }
}