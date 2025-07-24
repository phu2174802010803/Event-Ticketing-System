package com.example.eventservice.repository;

import com.example.eventservice.model.Favourite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavouriteRepository extends JpaRepository<Favourite, Integer> {
    Optional<Favourite> findByUserIdAndEventEventId(Integer userId, Integer eventId);

    Page<Favourite> findByUserId(Integer userId, Pageable pageable);

    void deleteByUserIdAndEventEventId(Integer userId, Integer eventId);
}