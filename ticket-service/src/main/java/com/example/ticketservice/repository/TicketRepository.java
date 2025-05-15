package com.example.ticketservice.repository;

import com.example.ticketservice.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Page<Ticket> findByUserIdAndStatus(Integer userId, String status, Pageable pageable);
    Page<Ticket> findByEventIdAndStatus(Integer eventId, String status, Pageable pageable);
    Optional<Ticket> findByTicketCode(String ticketCode);
}