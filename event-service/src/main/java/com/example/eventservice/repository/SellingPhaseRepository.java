package com.example.eventservice.repository;

import com.example.eventservice.model.SellingPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SellingPhaseRepository extends JpaRepository<SellingPhase, Integer> {
    List<SellingPhase> findByEventId(Integer eventId);

    List<SellingPhase> findByEventIdAndAreaId(Integer eventId, Integer areaId);

    @Modifying
    @Query("UPDATE SellingPhase sp SET sp.ticketsAvailable = :availableTickets WHERE sp.eventId = :eventId AND sp.phaseId = :phaseId")
    void updateAvailableTickets(@Param("eventId") Integer eventId, @Param("phaseId") Integer phaseId,
                                @Param("availableTickets") Integer availableTickets);
}