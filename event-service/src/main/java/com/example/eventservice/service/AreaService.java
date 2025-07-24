package com.example.eventservice.service;

import com.example.eventservice.dto.AreaRequestDto;
import com.example.eventservice.dto.AreaResponseDto;
import com.example.eventservice.model.Area;
import com.example.eventservice.model.Event;
import com.example.eventservice.repository.AreaRepository;
import com.example.eventservice.repository.EventRepository;
import com.example.eventservice.repository.TemplateAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AreaService {

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TemplateAreaRepository templateAreaRepository;

    public List<AreaResponseDto> getAreasByEvent(Integer eventId, String role) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        List<Area> areas = areaRepository.findAll().stream()
                .filter(area -> area.getEventId().equals(eventId))
                .collect(Collectors.toList());
        return areas.stream().map(this::convertToResponseDto).collect(Collectors.toList());
    }

    public List<AreaResponseDto> getAreasByEventForOrganizer(Integer eventId, Integer organizerId) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc bạn không có quyền truy cập"));
        List<Area> areas = areaRepository.findAll().stream()
                .filter(area -> area.getEventId().equals(eventId))
                .collect(Collectors.toList());
        return areas.stream().map(this::convertToResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public AreaResponseDto createArea(Integer eventId, AreaRequestDto requestDto, String role) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        if (requestDto.getTemplateAreaId() != null) {
            templateAreaRepository.findById(requestDto.getTemplateAreaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy template area"));
        }
        Area area = new Area();
        area.setEventId(eventId);
        area.setTemplateAreaId(requestDto.getTemplateAreaId());
        area.setName(requestDto.getName());
        area.setTotalTickets(requestDto.getTotalTickets());
        area.setAvailableTickets(requestDto.getTotalTickets());
        area.setPrice(requestDto.getPrice());
        Area savedArea = areaRepository.save(area);
        return convertToResponseDto(savedArea);
    }

    @Transactional
    public AreaResponseDto createAreaForOrganizer(Integer eventId, AreaRequestDto requestDto, Integer organizerId) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc bạn không có quyền truy cập"));
        if (requestDto.getTemplateAreaId() != null) {
            templateAreaRepository.findById(requestDto.getTemplateAreaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy template area"));
        }
        Area area = new Area();
        area.setEventId(eventId);
        area.setTemplateAreaId(requestDto.getTemplateAreaId());
        area.setName(requestDto.getName());
        area.setTotalTickets(requestDto.getTotalTickets());
        area.setAvailableTickets(requestDto.getTotalTickets());
        area.setPrice(requestDto.getPrice());
        Area savedArea = areaRepository.save(area);
        return convertToResponseDto(savedArea);
    }

    @Transactional
    public AreaResponseDto updateArea(Integer eventId, Integer areaId, AreaRequestDto requestDto, String role) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
        if (!area.getEventId().equals(eventId)) {
            throw new RuntimeException("Khu vực không thuộc sự kiện này");
        }
        if (requestDto.getTemplateAreaId() != null) {
            templateAreaRepository.findById(requestDto.getTemplateAreaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy template area"));
            area.setTemplateAreaId(requestDto.getTemplateAreaId());
        }
        area.setName(requestDto.getName());
        area.setTotalTickets(requestDto.getTotalTickets());
        area.setAvailableTickets(requestDto.getTotalTickets());
        area.setPrice(requestDto.getPrice());
        Area updatedArea = areaRepository.save(area);
        return convertToResponseDto(updatedArea);
    }

    @Transactional
    public AreaResponseDto updateAreaForOrganizer(Integer eventId, Integer areaId, AreaRequestDto requestDto, Integer organizerId) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc bạn không có quyền truy cập"));
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
        if (!area.getEventId().equals(eventId)) {
            throw new RuntimeException("Khu vực không thuộc sự kiện này");
        }
        if (requestDto.getTemplateAreaId() != null) {
            templateAreaRepository.findById(requestDto.getTemplateAreaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy template area"));
            area.setTemplateAreaId(requestDto.getTemplateAreaId());
        }
        area.setName(requestDto.getName());
        area.setTotalTickets(requestDto.getTotalTickets());
        area.setAvailableTickets(requestDto.getTotalTickets());
        area.setPrice(requestDto.getPrice());
        Area updatedArea = areaRepository.save(area);
        return convertToResponseDto(updatedArea);
    }

    @Transactional
    public void deleteArea(Integer eventId, Integer areaId, String role) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
        if (!area.getEventId().equals(eventId)) {
            throw new RuntimeException("Khu vực không thuộc sự kiện này");
        }
        areaRepository.delete(area);
    }

    @Transactional
    public void deleteAreaForOrganizer(Integer eventId, Integer areaId, Integer organizerId) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc bạn không có quyền truy cập"));
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
        if (!area.getEventId().equals(eventId)) {
            throw new RuntimeException("Khu vực không thuộc sự kiện này");
        }
        areaRepository.delete(area);
    }

    private AreaResponseDto convertToResponseDto(Area area) {
        AreaResponseDto dto = new AreaResponseDto();
        dto.setAreaId(area.getAreaId());
        dto.setEventId(area.getEventId());
        dto.setTemplateAreaId(area.getTemplateAreaId());
        dto.setName(area.getName());
        dto.setTotalTickets(area.getTotalTickets());
        dto.setAvailableTickets(area.getAvailableTickets());
        dto.setPrice(area.getPrice());
        return dto;
    }
}