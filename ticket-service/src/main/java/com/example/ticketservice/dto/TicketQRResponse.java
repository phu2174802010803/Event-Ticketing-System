package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketQRResponse {
    private String qrCode;

    public TicketQRResponse(String qrCode) {
        this.qrCode = qrCode;
    }
}
