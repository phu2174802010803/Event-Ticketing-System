package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketHistoryResponse {
    private Integer ticketId;
    private String eventName;
    private String area;
    private String status;
    private String ticketCode;
    private String purchaseDate;

    public TicketHistoryResponse(Integer ticketId, String eventName, String area, String status, String ticketCode, String purchaseDate) {
        this.ticketId = ticketId;
        this.eventName = eventName;
        this.area = area;
        this.status = status;
        this.ticketCode = ticketCode;
        this.purchaseDate = purchaseDate;
    }
}
