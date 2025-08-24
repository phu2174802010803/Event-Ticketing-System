package com.example.eventservice.controller;

import com.example.eventservice.dto.CategoryPublicListDto;
import com.example.eventservice.dto.EventPublicDetailDto;
import com.example.eventservice.dto.EventPublicListDto;
import com.example.eventservice.dto.SellingPhaseResponseDto;
import com.example.eventservice.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    // Lấy sự kiện có banner cho trang chủ
    @GetMapping("/banners")
    public ResponseEntity<List<EventPublicDetailDto>> getBanners() {
        List<EventPublicDetailDto> banners = eventService.getBannerEvents();
        return ResponseEntity.ok(banners);
    }

    // Lọc sự kiện công khai theo các tiêu chí
    @GetMapping("/filter")
    public ResponseEntity<List<EventPublicListDto>> filterPublicEvents(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String location) {
        List<EventPublicListDto> events = eventService.filterPublicEvents(categoryId, dateFrom, dateTo, location);
        return ResponseEntity.ok(events);
    }

    // Xem danh sách phiên bán vé công khai của sự kiện
    @GetMapping("/{eventId}/phases")
    public ResponseEntity<List<SellingPhaseResponseDto>> getSellingPhases(@PathVariable Integer eventId) {
        List<SellingPhaseResponseDto> phases = eventService.getPublicSellingPhasesByEvent(eventId);
        return ResponseEntity.ok(phases);
    }
}