package com.example.eventservice.controller;

import com.example.eventservice.dto.EventRequestDto;
import com.example.eventservice.dto.EventResponseDto;
import com.example.eventservice.model.Event;
import com.example.eventservice.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminEventController {

    @Autowired
    private EventService eventService;

    @PostMapping("/events")
    public ResponseEntity<EventResponseDto> createEvent(
            @Valid @RequestBody EventRequestDto requestDto,
            @RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token không hợp lệ");
        }
        String token = authorizationHeader.substring(7);
        Event event = eventService.createEventForAdmin(requestDto, token);
        return ResponseEntity.ok(new EventResponseDto(event.getEventId(), "Tạo sự kiện thành công"));
    }
}