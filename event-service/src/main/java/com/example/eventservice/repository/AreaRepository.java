package com.example.eventservice.repository;

import com.example.eventservice.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AreaRepository extends JpaRepository<Area, Integer> {
    void deleteByEventId(Integer eventId);

    @Modifying
    @Query("UPDATE Area a SET a.availableTickets = :availableTickets WHERE a.eventId = :eventId AND a.areaId = :areaId")
    void updateAvailableTickets(@Param("eventId") Integer eventId, @Param("areaId") Integer areaId, @Param("availableTickets") Integer availableTickets);
}