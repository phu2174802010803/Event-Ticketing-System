package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketPurchaseResponse {
    private String ticketCode;
    private String paymentUrl;
    private String message;
}