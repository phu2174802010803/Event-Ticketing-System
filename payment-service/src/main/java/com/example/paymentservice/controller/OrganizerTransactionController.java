package com.example.paymentservice.controller;

import com.example.paymentservice.dto.*;
import com.example.paymentservice.model.Transaction;
import com.example.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerTransactionController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/transactions")
    public ResponseEntity<ResponseWrapper<Page<TransactionSummaryDto>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authorizationHeader) {
        Integer organizerId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication()
                        .getPrincipal());
        String token = authorizationHeader.substring(7);
        Page<TransactionSummaryDto> transactions = paymentService.getOrganizerTransactionsSummary(organizerId,
                token,
                page, size);
        return ResponseEntity.ok(
                new ResponseWrapper<>("success", "Transactions retrieved successfully", transactions));
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ResponseWrapper<TransactionDetail>> getTransaction(
            @PathVariable String transactionId,
            @RequestHeader("Authorization") String authorizationHeader) {
        Integer organizerId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication()
                        .getPrincipal());
        String token = authorizationHeader.substring(7);
        TransactionDetail transaction = paymentService.getTransactionDetailForOrganizer(transactionId,
                organizerId,
                token);
        return ResponseEntity.ok(
                new ResponseWrapper<>("success", "Transaction retrieved successfully", transaction));
    }

    @GetMapping("/transactions/stats")
    public ResponseEntity<ResponseWrapper<TransactionStatsDto>> getTransactionStats(
            @RequestHeader("Authorization") String authorizationHeader) {
        Integer organizerId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication()
                        .getPrincipal());
        String token = authorizationHeader.substring(7);
        TransactionStatsDto stats = paymentService.getOrganizerTransactionStats(organizerId, token);
        return ResponseEntity.ok(new ResponseWrapper<>("success",
                "Transaction statistics retrieved successfully", stats));
    }

}