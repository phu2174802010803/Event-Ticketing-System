package com.example.eventservice.controller;

import com.example.eventservice.dto.EventPublicDetailDto;
import com.example.eventservice.dto.EventPublicListDto;
import com.example.eventservice.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/public")
public class PublicEventController {

    @Autowired
    private EventService eventService;

    // Xem danh sách sự kiện công khai
    @GetMapping
    public ResponseEntity<List<EventPublicListDto>> getPublicEvents() {
        List<EventPublicListDto> events = eventService.getPublicEvents();
        return ResponseEntity.ok(events);
    }

    // Xem chi tiết một sự kiện công khai
    @GetMapping("/{eventId}")
    public ResponseEntity<EventPublicDetailDto> getPublicEventDetail(@PathVariable Integer eventId) {
        EventPublicDetailDto eventDetail = eventService.getPublicEventDetail(eventId);
        return ResponseEntity.ok(eventDetail);
    }
}