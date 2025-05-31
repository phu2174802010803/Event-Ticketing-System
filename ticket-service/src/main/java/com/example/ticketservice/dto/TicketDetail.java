package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketDetail {
    private String ticketCode;
    private String status;
    private String purchaseDate;
    private Double price;
    private String eventName;
    private String areaName;

    // Constructor
    public TicketDetail() {
    }

    public TicketDetail(String ticketCode, String status, String purchaseDate, Double price, String eventName,
                        String areaName) {
        this.ticketCode = ticketCode;
        this.status = status;
        this.purchaseDate = purchaseDate;
        this.price = price;
        this.eventName = eventName;
        this.areaName = areaName;
    }
}