package com.example.eventservice.controller;

import com.example.eventservice.dto.EventDetailResponseDto;
import com.example.eventservice.dto.EventRequestDto;
import com.example.eventservice.dto.EventResponseDto;
import com.example.eventservice.dto.EventUpdateRequestDto;
import com.example.eventservice.model.Event;
import com.example.eventservice.service.AzureBlobStorageService;
import com.example.eventservice.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminEventController {

    @Autowired
    private EventService eventService;
    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    // Tạo sự kiện mới với hình ảnh
    @PostMapping(value = "/events", consumes = {"multipart/form-data"})
    public ResponseEntity<EventResponseDto> createEvent(
            @Valid @RequestPart("event") EventRequestDto requestDto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile) throws IOException {
        // Đảm bảo requestDto không có imageUrl ban đầu
        requestDto.setImageUrl(null);

        // Tạo sự kiện mà không có URL ảnh
        Event event = eventService.createEventForAdmin(requestDto);

        // Nếu có file ảnh và sự kiện được tạo thành công, upload ảnh và cập nhật URL
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = azureBlobStorageService.uploadImage(imageFile);
                event.setImageUrl(imageUrl);
                eventService.updateEventImageUrl(event.getEventId(), imageUrl);
            } catch (Exception e) {
                // Xử lý lỗi upload ảnh, có thể log lỗi hoặc thông báo cho người dùng
                throw new IOException("Không thể tải ảnh: " + e.getMessage(), e);
            }
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            String bannerUrl = azureBlobStorageService.uploadFile(bannerFile); // Sử dụng phương thức chung cho hình ảnh và video
            event.setBannerUrl(bannerUrl);
            eventService.updateEventBannerUrl(event.getEventId(), bannerUrl);
        }
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

    // Cập nhật sự kiện với hình ảnh (nếu có)
    @PutMapping(value = "/events/{eventId}", consumes = {"multipart/form-data"})
    public ResponseEntity<EventDetailResponseDto> updateEvent(
            @PathVariable Integer eventId,
            @RequestPart("event") EventUpdateRequestDto requestDto,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "banner", required = false) MultipartFile bannerFile) throws IOException {
        // Lấy sự kiện hiện tại để kiểm tra imageUrl cũ
        Event currentEvent = eventService.getEventById(eventId);
        String oldImageUrl = currentEvent.getImageUrl();
        String oldBannerUrl = currentEvent.getBannerUrl();

        // Xử lý logic hình ảnh
        if (imageFile != null && !imageFile.isEmpty()) {
            // Upload ảnh mới và thay thế ảnh cũ
            String imageUrl = azureBlobStorageService.uploadImage(imageFile);
            if (oldImageUrl != null) {
                azureBlobStorageService.deleteImage(oldImageUrl);
            }
            requestDto.setImageUrl(imageUrl);
        } else if (Boolean.TRUE.equals(requestDto.getRemoveImage())) {
            // Gỡ ảnh: đặt imageUrl thành null và xóa ảnh cũ trên Azure
            if (oldImageUrl != null) {
                azureBlobStorageService.deleteImage(oldImageUrl);
            }
            requestDto.setImageUrl(null);
        } else {
            // Giữ nguyên imageUrl cũ
            requestDto.setImageUrl(oldImageUrl);
        }

        // Xử lý logic banner giống như image
        if (bannerFile != null && !bannerFile.isEmpty()) {
            // Upload banner mới và thay thế banner cũ
            String bannerUrl = azureBlobStorageService.uploadFile(bannerFile);
            if (oldBannerUrl != null) {
                azureBlobStorageService.deleteImage(oldBannerUrl);
            }
            currentEvent.setBannerUrl(bannerUrl);
            eventService.updateEventBannerUrl(eventId, bannerUrl);
        } else if (Boolean.TRUE.equals(requestDto.getRemoveBanner())) {
            // Gỡ banner: đặt bannerUrl thành null và xóa banner cũ trên Azure
            if (oldBannerUrl != null) {
                azureBlobStorageService.deleteImage(oldBannerUrl);
            }
            currentEvent.setBannerUrl(null);
            eventService.updateEventBannerUrl(eventId, null);
        } else {
            // Giữ nguyên bannerUrl cũ
            currentEvent.setBannerUrl(oldBannerUrl);
        }

        // Cập nhật sự kiện
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