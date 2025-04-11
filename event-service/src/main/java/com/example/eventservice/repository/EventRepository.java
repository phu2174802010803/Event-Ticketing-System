package com.example.eventservice.repository;

import com.example.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer> {

    // Lấy danh sách tất cả sự kiện công khai (status = 'approved')
    @Query("SELECT e FROM Event e WHERE e.status = 'approved'")
    List<Event> findAllPublicEvents();

    // Lấy chi tiết một sự kiện công khai theo ID
    @Query("SELECT e FROM Event e WHERE e.eventId = :eventId AND e.status = 'approved'")
    Optional<Event> findPublicEventById(@Param("eventId") Integer eventId);
}