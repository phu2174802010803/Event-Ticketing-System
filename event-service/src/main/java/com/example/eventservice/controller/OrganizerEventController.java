package com.example.eventservice.controller;

import com.example.eventservice.dto.EventDetailResponseDto;
import com.example.eventservice.dto.EventRequestDto;
import com.example.eventservice.dto.EventResponseDto;
import com.example.eventservice.dto.EventUpdateRequestDto;
import com.example.eventservice.model.Event;
import com.example.eventservice.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerEventController {

    @Autowired
    private EventService eventService;

    //Tạo sự kiện
    @PostMapping("/events")
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody EventRequestDto requestDto) {
        Event event = eventService.createEventForOrganizer(requestDto);
        return ResponseEntity.ok(new EventResponseDto(event.getEventId(), "Tạo sự kiện thành công"));
    }

    // Xem danh sách sự kiện của Organizer
    @GetMapping("/events")
    public ResponseEntity<List<EventDetailResponseDto>> getEvents() {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        List<EventDetailResponseDto> events = eventService.getEventsByOrganizer(organizerId);
        return ResponseEntity.ok(events);
    }

    // Xem chi tiết sự kiện
    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventDetailResponseDto> getEventDetail(@PathVariable Integer eventId) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        EventDetailResponseDto event = eventService.getEventDetailForOrganizer(eventId, organizerId);
        return ResponseEntity.ok(event);
    }

    // Cập nhật sự kiện
    @PutMapping("/events/{eventId}")
    public ResponseEntity<EventDetailResponseDto> updateEvent(@PathVariable Integer eventId, @RequestBody EventUpdateRequestDto requestDto) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        EventDetailResponseDto updatedEvent = eventService.updateEventForOrganizer(eventId, organizerId, requestDto);
        return ResponseEntity.ok(updatedEvent);
    }

    // Xóa sự kiện
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<String> deleteEvent(@PathVariable Integer eventId) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        eventService.deleteEventForOrganizer(eventId, organizerId);
        return ResponseEntity.ok("Xóa sự kiện thành công");
    }

}