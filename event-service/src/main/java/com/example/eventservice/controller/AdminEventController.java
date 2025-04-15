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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminEventController {

    @Autowired
    private EventService eventService;
    // Tạo sự kiện mới
    @PostMapping("/events")
    public ResponseEntity<EventResponseDto> createEvent(@Valid @RequestBody EventRequestDto requestDto) {
        Event event = eventService.createEventForAdmin(requestDto);
        return ResponseEntity.ok(new EventResponseDto(event.getEventId(), "Tạo sự kiện thành công"));
    }

    // Xem danh sách tất cả sự kiện
    @GetMapping("/events")
    public ResponseEntity<List<EventDetailResponseDto>> getAllEvents() {
        List<EventDetailResponseDto> events = eventService.getAllEventsForAdmin();
        return ResponseEntity.ok(events);
    }

    // Xem chi tiết sự kiện
    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventDetailResponseDto> getEventDetail(@PathVariable Integer eventId) {
        EventDetailResponseDto event = eventService.getEventDetailForAdmin(eventId);
        return ResponseEntity.ok(event);
    }

    // Phê duyệt sự kiện
    @PutMapping("/events/{eventId}/approve")
    public ResponseEntity<EventDetailResponseDto> approveEvent(@PathVariable Integer eventId) {
        EventDetailResponseDto approvedEvent = eventService.approveEvent(eventId);
        return ResponseEntity.ok(approvedEvent);
    }

    // Từ chối sự kiện
    @PutMapping("/events/{eventId}/reject")
    public ResponseEntity<EventDetailResponseDto> rejectEvent(@PathVariable Integer eventId) {
        EventDetailResponseDto rejectedEvent = eventService.rejectEvent(eventId);
        return ResponseEntity.ok(rejectedEvent);
    }

    // Cập nhật sự kiện (pending)
    @PutMapping("/events/{eventId}")
    public ResponseEntity<EventDetailResponseDto> updateEvent(@PathVariable Integer eventId, @RequestBody EventUpdateRequestDto requestDto) {
        EventDetailResponseDto updatedEvent = eventService.updateEventForAdmin(eventId, requestDto);
        return ResponseEntity.ok(updatedEvent);
    }

    // Xóa sự kiện (pending)
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<String> deleteEvent(@PathVariable Integer eventId) {
        eventService.deleteEventForAdmin(eventId);
        return ResponseEntity.ok("Xóa sự kiện thành công");
    }
}