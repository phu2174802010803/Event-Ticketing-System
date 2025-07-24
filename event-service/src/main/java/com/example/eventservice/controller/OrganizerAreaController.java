package com.example.eventservice.controller;

import com.example.eventservice.dto.AreaRequestDto;
import com.example.eventservice.dto.AreaResponseDto;
import com.example.eventservice.dto.ResponseWrapper;
import com.example.eventservice.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerAreaController {

    @Autowired
    private AreaService areaService;

    @GetMapping("/events/{eventId}/areas")
    public ResponseEntity<ResponseWrapper<List<AreaResponseDto>>> getAreasByEvent(@PathVariable Integer eventId) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        List<AreaResponseDto> areas = areaService.getAreasByEventForOrganizer(eventId, organizerId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Danh sách khu vực", areas));
    }

    @PostMapping("/events/{eventId}/areas")
    public ResponseEntity<ResponseWrapper<AreaResponseDto>> createArea(
            @PathVariable Integer eventId,
            @Valid @RequestBody AreaRequestDto requestDto) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        AreaResponseDto area = areaService.createAreaForOrganizer(eventId, requestDto, organizerId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Thêm khu vực thành công", area));
    }

    @PutMapping("/events/{eventId}/areas/{areaId}")
    public ResponseEntity<ResponseWrapper<AreaResponseDto>> updateArea(
            @PathVariable Integer eventId,
            @PathVariable Integer areaId,
            @Valid @RequestBody AreaRequestDto requestDto) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        AreaResponseDto area = areaService.updateAreaForOrganizer(eventId, areaId, requestDto, organizerId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Cập nhật khu vực thành công", area));
    }

    @DeleteMapping("/events/{eventId}/areas/{areaId}")
    public ResponseEntity<ResponseWrapper<String>> deleteArea(
            @PathVariable Integer eventId,
            @PathVariable Integer areaId) {
        Integer organizerId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        areaService.deleteAreaForOrganizer(eventId, areaId, organizerId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Xóa khu vực thành công", "Khu vực đã được xóa"));
    }
}