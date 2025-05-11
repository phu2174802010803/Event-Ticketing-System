package com.example.eventservice.repository;

import com.example.eventservice.model.SellingPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SellingPhaseRepository extends JpaRepository<SellingPhase, Integer> {
    List<SellingPhase> findByEventId(Integer eventId);
    List<SellingPhase> findByEventIdAndAreaId(Integer eventId, Integer areaId);
}