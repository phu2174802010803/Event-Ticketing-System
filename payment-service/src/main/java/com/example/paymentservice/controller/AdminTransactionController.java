package com.example.paymentservice.controller;

import com.example.paymentservice.dto.FinancialReportDto;
import com.example.paymentservice.dto.ResponseWrapper;
import com.example.paymentservice.dto.TransactionResponseDto;
import com.example.paymentservice.dto.TransactionDetail;
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
    public ResponseEntity<ResponseWrapper<List<TransactionDetail>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer userId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        // Nếu có userId parameter, trả về transactions với tickets
        if (userId != null) {
            String token = authorizationHeader != null ? authorizationHeader.substring(7) : "";
            List<TransactionDetail> data = paymentService.getTransactionsWithTicketsByUserId(userId, token);
            return ResponseEntity.ok(new ResponseWrapper<>("success", "All transactions retrieved successfully", data));
        }

        // Nếu không có userId, trả về tất cả transactions (backward compatibility)
        Page<Transaction> transactions = paymentService.getAllTransactions(page, size);
        List<TransactionDetail> data = transactions.stream()
                .map(transaction -> {
                    TransactionDetail dto = new TransactionDetail();
                    dto.setTransactionId(transaction.getTransactionId());
                    dto.setEventId(transaction.getEventId());
                    dto.setTotalAmount(transaction.getTotalAmount());
                    dto.setPaymentMethod(transaction.getPaymentMethod());
                    dto.setStatus(transaction.getStatus());
                    dto.setTransactionDate(transaction.getTransactionDate().toString());
                    dto.setTickets(null); // No tickets for general list
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ResponseWrapper<>("success", "All transactions retrieved successfully", data));
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ResponseWrapper<TransactionResponseDto>> getTransaction(
            @PathVariable String transactionId) {
        Transaction transaction = paymentService.getTransactionById(transactionId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Transaction retrieved successfully",
                paymentService.toResponseDto(transaction)));
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
}