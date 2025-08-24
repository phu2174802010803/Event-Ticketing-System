package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class TicketSelectionResponse {
    private String transactionId;
    private String eventName;
    private String areaName;
    private String message;
    private Long ttl; // Time to live in seconds
}