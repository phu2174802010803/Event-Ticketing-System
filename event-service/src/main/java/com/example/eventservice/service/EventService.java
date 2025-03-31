package com.example.eventservice.service;

import com.example.eventservice.dto.EventRequestDto;
import com.example.eventservice.model.Area;
import com.example.eventservice.model.Event;
import com.example.eventservice.repository.AreaRepository;
import com.example.eventservice.repository.EventRepository;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private IdentityClient identityClient;

    @Transactional
    public Event createEventForOrganizer(EventRequestDto requestDto, String token) {
        Integer organizerId = identityClient.getUserId(token); // get user id from token
        Event event = new Event();
        event.setOrganizerId(organizerId); // set organizer id
        event.setCategoryId(requestDto.getCategoryId()); // set category id
        event.setName(requestDto.getName());
        event.setDescription(requestDto.getDescription());
        event.setDate(requestDto.getDate());
        event.setTime(requestDto.getTime());
        event.setLocation(requestDto.getLocation());
        event.setMapTemplateId(requestDto.getMapTemplateId());
        event.setImageUrl(requestDto.getImageUrl());
        event.setStatus("pending"); // Mặc định cho Organizer
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        Event saveEvent = eventRepository.save(event); // save event

        //Lưu các khu vực của sự kiện
        if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
            for (var areaDto : requestDto.getAreas()) {
                Area area = new Area();
                area.setEventId(saveEvent.getEventId()); // set event id
                area.setTemplateAreaId(areaDto.getTemplateAreaId()); // set template area id
                area.setName(areaDto.getName()); // set name
                area.setTotalTickets(areaDto.getTotalTickets()); // set total tickets
                area.setAvailableTickets(areaDto.getTotalTickets()); // ban đầu số vé còn lại bằng tổng số vé
                area.setPrice(areaDto.getPrice()); // set price
                areaRepository.save(area); // save area
            }
        }
        return saveEvent;
    }

    @Transactional
    public Event createEventForAdmin(EventRequestDto requestDto, String token) {
        Integer organizerId = identityClient.getUserId(token);
        Event event = new Event();
        event.setOrganizerId(organizerId);
        event.setCategoryId(requestDto.getCategoryId());
        event.setName(requestDto.getName());
        event.setDescription(requestDto.getDescription());
        event.setDate(requestDto.getDate());
        event.setTime(requestDto.getTime());
        event.setLocation(requestDto.getLocation());
        event.setMapTemplateId(requestDto.getMapTemplateId());
        event.setImageUrl(requestDto.getImageUrl());
        String status = requestDto.getStatus() != null ? requestDto.getStatus() : "pending";
        if (!status.equals("pending") && !status.equals("approved") && !status.equals("rejected")) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ, chỉ có thể là: pending, approved, rejected");
        }
        event.setStatus(status);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);

        // Lưu các khu vực
        if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
            for (var areaDto : requestDto.getAreas()) {
                Area area = new Area();
                area.setEventId(savedEvent.getEventId());
                area.setTemplateAreaId(areaDto.getTemplateAreaId());
                area.setName(areaDto.getName());
                area.setTotalTickets(areaDto.getTotalTickets());
                area.setAvailableTickets(areaDto.getTotalTickets()); // Ban đầu bằng total_tickets
                area.setPrice(areaDto.getPrice());
                areaRepository.save(area);
            }
        }

        return savedEvent;
    }
}