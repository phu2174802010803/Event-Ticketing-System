package com.example.identityservice.repository;

import com.example.identityservice.model.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizerRepository extends JpaRepository<Organizer, Integer> {
    Optional<Organizer> findByUserId(Integer userId);
}