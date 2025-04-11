package com.example.eventservice.service;

import com.example.eventservice.dto.EventPublicDetailDto;
import com.example.eventservice.dto.EventPublicListDto;
import com.example.eventservice.dto.EventRequestDto;
import com.example.eventservice.model.Area;
import com.example.eventservice.model.Event;
import com.example.eventservice.model.TemplateArea;
import com.example.eventservice.repository.AreaRepository;
import com.example.eventservice.repository.EventRepository;
import com.example.eventservice.repository.TemplateAreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TemplateAreaRepository templateAreaRepository;

    //Lấy danh sách sự kiện công khai
    public List<EventPublicListDto> getPublicEvents() {
        List<Event> events = eventRepository.findAllPublicEvents();
        return events.stream()
                .map(this::convertToPublicListDto) //chuyển đổi sang DTO
                .collect(Collectors.toList());
    }

    // Lấy chi tiết một sự kiện công khai
    public EventPublicDetailDto getPublicEventDetail(Integer eventId) {
        Event event = eventRepository.findPublicEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc sự kiện chưa được phê duyệt"));
        return convertToPublicDetailDto(event);
    }

    //Chuyển đổi Event thành EventPublicListDto
    private EventPublicListDto convertToPublicListDto(Event event) {
        EventPublicListDto dto = new EventPublicListDto();
        dto.setEventId(event.getEventId());
        dto.setName(event.getName());
        dto.setDate(event.getDate());
        dto.setTime(event.getTime());
        dto.setLocation(event.getLocation());
        dto.setImageUrl(event.getImageUrl());
        return dto;
    }

    // Chuyển đổi Event thành EventPublicDetailDto
    private EventPublicDetailDto convertToPublicDetailDto(Event event) {
        EventPublicDetailDto dto = new EventPublicDetailDto();
        dto.setEventId(event.getEventId());
        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setDate(event.getDate());
        dto.setTime(event.getTime());
        dto.setLocation(event.getLocation());
        dto.setImageUrl(event.getImageUrl());
        dto.setCategory(categoryService.getCategoryById(event.getCategoryId()));
        return dto;
    }


    @Transactional
    public Event createEventForOrganizer(EventRequestDto requestDto) {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer organizerId = Integer.parseInt(userIdStr);

        //Kiểm tra category_id
        if (requestDto.getCategoryId() != null && !categoryService.categoryExists(requestDto.getCategoryId())) {
            throw new IllegalArgumentException("Danh mục không tồn tại");
        }
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
        event.setStatus("pending");
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);

        // Lấy danh sách template_areas từ map_template_id
        List<TemplateArea> templateAreas = templateAreaRepository.findByMapTemplateTemplateId(requestDto.getMapTemplateId());
        if (templateAreas.isEmpty()) {
            throw new IllegalArgumentException("Template map không có khu vực nào");
        }

        if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
            for (var areaDto : requestDto.getAreas()) {
                TemplateArea matchingTemplateArea = templateAreas.stream()
                        .filter(ta -> ta.getTemplateAreaId().equals(areaDto.getTemplateAreaId())) //Sử dụng bộ lọc để tìm Id trùng với Id client truyền vào
                        .findFirst()// Lấy phần tử đầu tiên
                        .orElseThrow(() -> new IllegalArgumentException("template_area_id không hợp lệ: " + areaDto.getTemplateAreaId())); // Nếu không tìm thấy, ném ra ngoại lệ

                Area area = new Area();
                area.setEventId(savedEvent.getEventId());
                area.setTemplateAreaId(matchingTemplateArea.getTemplateAreaId());
                area.setName(areaDto.getName() != null ? areaDto.getName() : matchingTemplateArea.getName()); //Toán tử 3 ngôi, đảm bảo tên khu vực không null
                area.setTotalTickets(areaDto.getTotalTickets());
                area.setAvailableTickets(areaDto.getTotalTickets());
                area.setPrice(areaDto.getPrice());
                areaRepository.save(area);
            }
        }
        return savedEvent;
    }

    @Transactional
    public Event createEventForAdmin(EventRequestDto requestDto) {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer organizerId = Integer.parseInt(userIdStr);
        //Kiểm tra category_id
        if (requestDto.getCategoryId() != null && !categoryService.categoryExists(requestDto.getCategoryId())) {
            throw new IllegalArgumentException("Danh mục không tồn tại");
        }
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
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }
        event.setStatus(status);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);

        // Lấy danh sách template_areas từ map_template_id
        List<TemplateArea> templateAreas = templateAreaRepository.findByMapTemplateTemplateId(requestDto.getMapTemplateId());
        if (templateAreas.isEmpty()) {
            throw new IllegalArgumentException("Template map không có khu vực nào");
        }

        if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
            for (var areaDto : requestDto.getAreas()) {
                TemplateArea matchingTemplateArea = templateAreas.stream()
                        .filter(ta -> ta.getTemplateAreaId().equals(areaDto.getTemplateAreaId())) //Sử dụng bộ lọc để tìm Id trùng với Id client truyền vào
                        .findFirst()// Lấy phần tử đầu tiên
                        .orElseThrow(() -> new IllegalArgumentException("template_area_id không hợp lệ: " + areaDto.getTemplateAreaId())); // Nếu không tìm thấy, ném ra ngoại lệ

                Area area = new Area();
                area.setEventId(savedEvent.getEventId());
                area.setTemplateAreaId(matchingTemplateArea.getTemplateAreaId());
                area.setName(areaDto.getName() != null ? areaDto.getName() : matchingTemplateArea.getName()); //Toán tử 3 ngôi, đảm bảo tên khu vực không null
                area.setTotalTickets(areaDto.getTotalTickets());
                area.setAvailableTickets(areaDto.getTotalTickets());
                area.setPrice(areaDto.getPrice());
                areaRepository.save(area);
            }
        }
        return savedEvent;
    }
}