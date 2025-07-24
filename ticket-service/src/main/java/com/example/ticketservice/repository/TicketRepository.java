package com.example.ticketservice.repository;

import com.example.ticketservice.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Page<Ticket> findByUserId(Integer userId, Pageable pageable);

    Page<Ticket> findByUserIdAndStatus(Integer userId, String status, Pageable pageable);

    Page<Ticket> findByEventId(Integer eventId, Pageable pageable);

    Page<Ticket> findByEventIdAndStatus(Integer eventId, String status, Pageable pageable);

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByEventId(Integer eventId);

    Page<Ticket> findByStatus(String status, Pageable pageable);

    List<Ticket> findByTransactionId(String transactionId);

    // Statistics methods for better performance
    long countByStatus(String status);

    long countByEventIdIn(List<Integer> eventIds);

    long countByEventIdInAndStatus(List<Integer> eventIds, String status);

    @Query("SELECT SUM(t.price) FROM Ticket t WHERE t.status IN ('sold', 'used')")
    Double sumPriceForSoldAndUsedTickets();

    @Query("SELECT SUM(t.price) FROM Ticket t WHERE t.eventId IN :eventIds AND t.status IN ('sold', 'used')")
    Double sumPriceForSoldAndUsedTicketsByEventIds(@Param("eventIds") List<Integer> eventIds);
}