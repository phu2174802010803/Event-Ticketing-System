package com.example.paymentservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "event_id")
    private Integer eventId;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false)
    private String status;
}