package com.example.paymentservice.controller;

import com.example.paymentservice.dto.*;
import com.example.paymentservice.model.Transaction;
import com.example.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminTransactionController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/transactions")
    public ResponseEntity<ResponseWrapper<Page<TransactionSummaryDto>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer userId,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        Page<TransactionSummaryDto> transactions = paymentService.getAllTransactionsSummary(page, size, userId,
                token);
        return ResponseEntity.ok(
                new ResponseWrapper<>("success", "All transactions retrieved successfully",
                        transactions));
    }

    @GetMapping("/transactions/details")
    public ResponseEntity<ResponseWrapper<Page<TransactionDetail>>> getAllTransactionsDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer userId,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        Page<TransactionDetail> transactions = paymentService.getAllTransactions(page, size, userId, token);
        return ResponseEntity.ok(
                new ResponseWrapper<>("success", "All transactions with details retrieved successfully",
                        transactions));
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ResponseWrapper<TransactionDetail>> getTransaction(
            @PathVariable String transactionId,
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.substring(7);
        TransactionDetail transaction = paymentService.getTransactionDetail(transactionId, token);
        return ResponseEntity.ok(
                new ResponseWrapper<>("success", "Enhanced transaction details retrieved successfully",
                        transaction));
    }

    @GetMapping("/reports/financial")
    public ResponseEntity<ResponseWrapper<FinancialReportDto>> generateFinancialReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDateTime start = LocalDateTime.parse(startDate).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = LocalDateTime.parse(endDate).withHour(23).withMinute(59).withSecond(59);

        System.out.println("Processing financial report request from " + start + " to " + end);

        FinancialReportDto report = paymentService.generateFinancialReport(start, end);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Financial report generated successfully", report));
    }

    @GetMapping("/transactions/stats")
    public ResponseEntity<ResponseWrapper<TransactionStatsDto>> getTransactionStats(
            @RequestHeader("Authorization") String authorizationHeader) {
        TransactionStatsDto stats = paymentService.getAdminTransactionStats();
        return ResponseEntity.ok(new ResponseWrapper<>("success",
                "Transaction statistics retrieved successfully", stats));
    }
}