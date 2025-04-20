package com.example.eventservice.controller;

import com.example.eventservice.dto.CategoryPublicListDto;
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

    // Xem danh sách danh mục công khai
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryPublicListDto>> getPublicCategories() {
        List<CategoryPublicListDto> categories = eventService.getPublicCategories();
        return ResponseEntity.ok(categories);
    }

    // Lấy sự kiện theo danh mục
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<EventPublicListDto>> getPublicEventsByCategory(@PathVariable Integer categoryId) {
        List<EventPublicListDto> events = eventService.getPublicEventsByCategory(categoryId);
        return ResponseEntity.ok(events);
    }

    // Lấy sự kiện nổi bật
    @GetMapping("/featured")
    public ResponseEntity<List<EventPublicListDto>> getFeaturedPublicEvents() {
        List<EventPublicListDto> events = eventService.getFeaturedPublicEvents();
        return ResponseEntity.ok(events);
    }

    // Tìm kiếm sự kiện công khai theo tên hoặc địa điểm
    @GetMapping("/search")
    public ResponseEntity<List<EventPublicListDto>> searchPublicEvents(@RequestParam String keyword) {
        List<EventPublicListDto> events = eventService.searchPublicEvents(keyword);
        return ResponseEntity.ok(events);
    }
}