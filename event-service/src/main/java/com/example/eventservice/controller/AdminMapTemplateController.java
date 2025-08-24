package com.example.eventservice.controller;

import com.example.eventservice.dto.MapTemplateRequestDto;
import com.example.eventservice.dto.MapTemplateResponseDto;
import com.example.eventservice.service.MapTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminMapTemplateController {

    @Autowired
    private MapTemplateService mapTemplateService;

    @PostMapping("/map-templates")
    public ResponseEntity<MapTemplateResponseDto> createTemplate(@Valid @RequestBody MapTemplateRequestDto requestDto) {
        MapTemplateResponseDto response = mapTemplateService.createTemplate(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/map-templates")
    public ResponseEntity<List<MapTemplateResponseDto>> getAllTemplates() {
        List<MapTemplateResponseDto> templates = mapTemplateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/map-templates/{templateId}")
    public ResponseEntity<MapTemplateResponseDto> getTemplateById(@PathVariable Integer templateId) {
        MapTemplateResponseDto response = mapTemplateService.getTemplateById(templateId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/map-templates/{templateId}")
    public ResponseEntity<MapTemplateResponseDto> updateTemplate(@PathVariable Integer templateId,
                                                                 @Valid @RequestBody MapTemplateRequestDto requestDto) {
        MapTemplateResponseDto response = mapTemplateService.updateTemplate(templateId, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/map-templates/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Integer templateId) {
        mapTemplateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}