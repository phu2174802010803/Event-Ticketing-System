package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String transactionId;
    private Double amount;
    private String paymentMethod;
    private Integer eventId;
}
