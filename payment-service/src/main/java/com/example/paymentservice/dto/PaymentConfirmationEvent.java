package com.example.paymentservice.dto;

import lombok.Data;

@Data
public class PaymentConfirmationEvent {
    private String transactionId;
    private String status;
    private Integer userId;
    private Integer eventId;
}