package com.example.eventservice.controller;

import com.example.eventservice.dto.ResponseWrapper;
import com.example.eventservice.dto.TemplateAreaRequestDto;
import com.example.eventservice.dto.TemplateAreaResponseDto;
import com.example.eventservice.service.MapTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminMapTemplateAreaController {

    @Autowired
    private MapTemplateService mapTemplateService;

    // Các endpoint mới cho quản lý khu vực
    @GetMapping("/map-templates/{templateId}/areas")
    public ResponseEntity<ResponseWrapper<List<TemplateAreaResponseDto>>> getAreasByTemplate(
            @PathVariable Integer templateId) {
        List<TemplateAreaResponseDto> areas = mapTemplateService.getAreasByTemplate(templateId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Danh sách khu vực của mẫu map", areas));
    }

    @PostMapping("/map-templates/{templateId}/areas")
    public ResponseEntity<ResponseWrapper<TemplateAreaResponseDto>> createArea(
            @PathVariable Integer templateId,
            @Valid @RequestBody TemplateAreaRequestDto requestDto) {
        TemplateAreaResponseDto area = mapTemplateService.createArea(templateId, requestDto);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Thêm khu vực thành công", area));
    }

    @PutMapping("/map-templates/{templateId}/areas/{areaId}")
    public ResponseEntity<ResponseWrapper<TemplateAreaResponseDto>> updateArea(
            @PathVariable Integer templateId,
            @PathVariable Integer areaId,
            @Valid @RequestBody TemplateAreaRequestDto requestDto) {
        TemplateAreaResponseDto area = mapTemplateService.updateArea(templateId, areaId, requestDto);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Cập nhật khu vực thành công", area));
    }

    @DeleteMapping("/map-templates/{templateId}/areas/{areaId}")
    public ResponseEntity<ResponseWrapper<String>> deleteArea(
            @PathVariable Integer templateId,
            @PathVariable Integer areaId) {
        mapTemplateService.deleteArea(templateId, areaId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Xóa khu vực thành công", "Khu vực đã được xóa"));
    }
}