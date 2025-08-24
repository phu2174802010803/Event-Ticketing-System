package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketScanResponse {
    private String message;
    private String status;

    public TicketScanResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }
    //
}
