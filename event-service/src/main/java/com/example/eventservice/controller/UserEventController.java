package com.example.eventservice.controller;

import com.example.eventservice.dto.AreaDetailDto;
import com.example.eventservice.dto.MapDetailDto;
import com.example.eventservice.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class UserEventController {

    @Autowired
    private EventService eventService;

    @GetMapping("/{eventId}/map")
    public ResponseEntity<MapDetailDto> getEventMap(@PathVariable Integer eventId) {
        Integer userId = Integer.parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        MapDetailDto mapDetail = eventService.getEventMap(eventId, userId);
        return ResponseEntity.ok(mapDetail);
    }

    @GetMapping("/{eventId}/areas/{areaId}")
    public ResponseEntity<AreaDetailDto> getAreaDetail(
            @PathVariable Integer eventId,
            @PathVariable Integer areaId) {
        AreaDetailDto areaDetail = eventService.getAreaDetail(eventId, areaId);
        return ResponseEntity.ok(areaDetail);
    }
}