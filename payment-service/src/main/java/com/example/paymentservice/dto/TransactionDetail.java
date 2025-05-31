package com.example.paymentservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class TransactionDetail {
    private String transactionId;
    private Integer eventId;
    private Double totalAmount;
    private String paymentMethod;
    private String status;
    private String transactionDate;
    private List<Object> tickets; // Use Object type to avoid cross-service dependency

    // Constructors
    public TransactionDetail() {
    }

    public TransactionDetail(String transactionId, Integer eventId, Double totalAmount, String paymentMethod,
                             String status, String transactionDate, List<Object> tickets) {
        this.transactionId = transactionId;
        this.eventId = eventId;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.transactionDate = transactionDate;
        this.tickets = tickets;
    }
}