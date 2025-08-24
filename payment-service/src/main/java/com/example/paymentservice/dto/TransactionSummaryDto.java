package com.example.paymentservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransactionSummaryDto {
    private String transactionId;
    private Integer userId;
    private String userName;
    private String userEmail;
    private Integer eventId;
    private String eventName;
    private String organizerName;
    private String organizerEmail;
    private Double totalAmount;
    private String paymentMethod;
    private String status;
    private LocalDateTime transactionDate;
}