package com.example.paymentservice.repository;

import com.example.paymentservice.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByEventIdIn(List<Integer> eventIds, Pageable pageable);

    Page<Transaction> findAll(Pageable pageable);

    Page<Transaction> findByUserId(Integer userId, Pageable pageable);

    List<Transaction> findByUserId(Integer userId);

    @Query("SELECT SUM(t.totalAmount), COUNT(t) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    Object[] calculateFinancialSummary(LocalDateTime startDate, LocalDateTime endDate);
}