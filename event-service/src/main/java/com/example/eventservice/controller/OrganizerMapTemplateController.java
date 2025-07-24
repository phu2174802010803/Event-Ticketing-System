package com.example.eventservice.controller;

import com.example.eventservice.dto.MapTemplateResponseDto;
import com.example.eventservice.service.MapTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerMapTemplateController {

    @Autowired
    private MapTemplateService mapTemplateService;

    @GetMapping("/map-templates")
    public ResponseEntity<List<MapTemplateResponseDto>> getAllTemplates() {
        List<MapTemplateResponseDto> templates = mapTemplateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }
}