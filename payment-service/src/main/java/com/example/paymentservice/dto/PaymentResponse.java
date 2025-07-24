package com.example.paymentservice.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentUrl;
    private String status;
    private String message;

    public PaymentResponse(String paymentUrl, String status, String message) {
        this.paymentUrl = paymentUrl;
        this.status = status;
        this.message = message;
    }
}