package com.example.paymentservice.repository;

import com.example.paymentservice.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Statistics methods for better performance
    long countByStatus(String status);

    long countByEventIdIn(List<Integer> eventIds);

    long countByEventIdInAndStatus(List<Integer> eventIds, String status);

    @Query("SELECT SUM(t.totalAmount) FROM Transaction t WHERE t.status = :status")
    Double sumAmountByStatus(@Param("status") String status);

    @Query("SELECT SUM(t.totalAmount) FROM Transaction t WHERE t.eventId IN :eventIds AND t.status = :status")
    Double sumAmountByEventIdInAndStatus(@Param("eventIds") List<Integer> eventIds, @Param("status") String status);
}