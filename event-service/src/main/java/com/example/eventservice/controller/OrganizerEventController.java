package com.example.eventservice.controller;

import com.example.eventservice.dto.EventRequestDto;

import com.example.eventservice.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerEventController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private EventService eventService;

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody EventRequestDto eventRequestDto) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7); // Bỏ "Bearer "
        return ResponseEntity.ok(eventService.createEventForOrganizer(eventRequestDto, token));
    }
}
