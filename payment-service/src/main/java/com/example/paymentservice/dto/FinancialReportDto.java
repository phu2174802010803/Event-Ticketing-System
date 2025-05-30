package com.example.paymentservice.dto;

import lombok.Data;

@Data
public class FinancialReportDto {
    private Double totalRevenue;
    private Integer totalTransactions;
    private Double averageTransactionAmount;
}