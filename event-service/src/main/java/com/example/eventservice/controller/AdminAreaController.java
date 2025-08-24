package com.example.eventservice.controller;

import com.example.eventservice.dto.AreaRequestDto;
import com.example.eventservice.dto.AreaResponseDto;
import com.example.eventservice.dto.ResponseWrapper;
import com.example.eventservice.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminAreaController {

    @Autowired
    private AreaService areaService;

    @GetMapping("/events/{eventId}/areas")
    public ResponseEntity<ResponseWrapper<List<AreaResponseDto>>> getAreasByEvent(@PathVariable Integer eventId) {
        List<AreaResponseDto> areas = areaService.getAreasByEvent(eventId, "ADMIN");
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Danh sách khu vực", areas));
    }

    @PostMapping("/events/{eventId}/areas")
    public ResponseEntity<ResponseWrapper<AreaResponseDto>> createArea(
            @PathVariable Integer eventId,
            @Valid @RequestBody AreaRequestDto requestDto) {
        AreaResponseDto area = areaService.createArea(eventId, requestDto, "ADMIN");
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Thêm khu vực thành công", area));
    }

    @PutMapping("/events/{eventId}/areas/{areaId}")
    public ResponseEntity<ResponseWrapper<AreaResponseDto>> updateArea(
            @PathVariable Integer eventId,
            @PathVariable Integer areaId,
            @Valid @RequestBody AreaRequestDto requestDto) {
        AreaResponseDto area = areaService.updateArea(eventId, areaId, requestDto, "ADMIN");
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Cập nhật khu vực thành công", area));
    }

    @DeleteMapping("/events/{eventId}/areas/{areaId}")
    public ResponseEntity<ResponseWrapper<String>> deleteArea(
            @PathVariable Integer eventId,
            @PathVariable Integer areaId) {
        areaService.deleteArea(eventId, areaId, "ADMIN");
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Xóa khu vực thành công", "Khu vực đã được xóa"));
    }
}