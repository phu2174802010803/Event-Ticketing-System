package com.example.ticketservice.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentUrl;
    private String status;
    private String message;
}
