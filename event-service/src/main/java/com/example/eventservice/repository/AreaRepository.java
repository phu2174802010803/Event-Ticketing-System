package com.example.eventservice.repository;

import com.example.eventservice.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Integer> {
    void deleteByEventId(Integer eventId);
}