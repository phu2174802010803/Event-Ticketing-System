package com.example.paymentservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransactionResponseDto {
    private String transactionId;
    private Integer userId;
    private Integer eventId;
    private Double totalAmount;
    private String paymentMethod;
    private String status;
    private LocalDateTime transactionDate;
}