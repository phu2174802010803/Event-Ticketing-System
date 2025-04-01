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
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody EventRequestDto requestDto) {
        Event event = eventService.createEventForAdmin(requestDto);
        return ResponseEntity.ok(new EventResponseDto(event.getEventId(), "Tạo sự kiện thành công"));
    }
}