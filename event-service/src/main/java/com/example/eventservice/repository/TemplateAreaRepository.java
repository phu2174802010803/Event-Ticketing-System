package com.example.eventservice.repository;

import com.example.eventservice.model.TemplateArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateAreaRepository extends JpaRepository<TemplateArea, Integer> {
    List<TemplateArea> findByMapTemplateTemplateId(Integer templateId);
}