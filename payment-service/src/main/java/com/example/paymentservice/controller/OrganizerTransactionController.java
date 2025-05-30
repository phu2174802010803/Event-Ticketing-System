package com.example.paymentservice.controller;

import com.example.paymentservice.dto.ResponseWrapper;
import com.example.paymentservice.dto.TransactionResponseDto;
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
    public ResponseEntity<ResponseWrapper<List<TransactionResponseDto>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String token) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Page<Transaction> transactions = paymentService.getOrganizerTransactions(organizerId, token.substring(7), page, size);
        List<TransactionResponseDto> data = transactions.stream()
                .map(paymentService::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Transactions retrieved successfully", data));
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ResponseWrapper<TransactionResponseDto>> getTransaction(
            @PathVariable String transactionId,
            @RequestHeader("Authorization") String token) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        Transaction transaction = paymentService.getTransactionById(transactionId);
        List<Integer> eventIds = paymentService.getEventClient().getOrganizerEventIds(organizerId, token.substring(7));
        if (!eventIds.contains(transaction.getEventId())) {
            throw new IllegalStateException("Transaction does not belong to your events");
        }
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Transaction retrieved successfully", paymentService.toResponseDto(transaction)));
    }

}