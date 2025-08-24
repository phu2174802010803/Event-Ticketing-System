package com.example.eventservice.repository;

import com.example.eventservice.model.MapTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapTemplateRepository extends JpaRepository<MapTemplate, Integer> {
    boolean existsByName(String name);
}