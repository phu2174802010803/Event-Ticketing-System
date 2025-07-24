package com.example.paymentservice.dto;

import lombok.Data;

@Data
public class TransactionStatsDto {
    private long totalTransactions;
    private long completedTransactions;
    private long pendingTransactions;
    private long failedTransactions;
    private double totalRevenue;
    private double pendingRevenue;

    public TransactionStatsDto() {
    }

    public TransactionStatsDto(long totalTransactions, long completedTransactions, long pendingTransactions,
                               long failedTransactions, double totalRevenue, double pendingRevenue) {
        this.totalTransactions = totalTransactions;
        this.completedTransactions = completedTransactions;
        this.pendingTransactions = pendingTransactions;
        this.failedTransactions = failedTransactions;
        this.totalRevenue = totalRevenue;
        this.pendingRevenue = pendingRevenue;
    }
}