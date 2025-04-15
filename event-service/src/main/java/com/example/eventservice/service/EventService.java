package com.example.eventservice.service;

import com.example.eventservice.dto.*;
import com.example.eventservice.model.Area;
import com.example.eventservice.model.Category;
import com.example.eventservice.model.Event;
import com.example.eventservice.model.TemplateArea;
import com.example.eventservice.repository.AreaRepository;
import com.example.eventservice.repository.CategoryRepository;
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
    @Autowired
    private CategoryRepository categoryRepository;

    //Lấy danh sách sự kiện công khai dành cho User
    public List<EventPublicListDto> getPublicEvents() {
        List<Event> events = eventRepository.findAllPublicEvents();
        return events.stream()
                .map(this::convertToPublicListDto) //chuyển đổi sang DTO
                .collect(Collectors.toList());
    }

    // Lấy danh sách danh mục công khai
    public List<CategoryPublicListDto> getPublicCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::convertToPublicCategoryListDto)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết một sự kiện công khai
    public EventPublicDetailDto getPublicEventDetail(Integer eventId) {
        Event event = eventRepository.findPublicEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc sự kiện chưa được phê duyệt"));
        return convertToPublicDetailDto(event);
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

    // Lấy danh sách sự kiện của Organizer
    public List<EventDetailResponseDto> getEventsByOrganizer(Integer organizerId) {
        List<Event> events = eventRepository.findByOrganizerId(organizerId);
        return events.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết sự kiện của Organizer
    public EventDetailResponseDto getEventDetailForOrganizer(Integer eventId, Integer organizerId) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        return convertToDetailDto(event);
    }

    // Cập nhật sự kiện của Organizer (pending)
    @Transactional
    public EventDetailResponseDto updateEventForOrganizer(Integer eventId, Integer organizerId, EventUpdateRequestDto requestDto) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        // Reset trạng thái về "pending" để chờ phê duyệt lại
        event.setStatus("pending");
        updateEventFields(event, requestDto);
        eventRepository.save(event);
        return convertToDetailDto(event);
    }


    // Xóa sự kiện của Organizer (pending)
    @Transactional
    public void deleteEventForOrganizer(Integer eventId, Integer organizerId) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        if (!"pending".equals(event.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xóa sự kiện ở trạng thái pending");
        }
        // Xóa các areas liên kết trước
        areaRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    // Lấy danh sách tất cả sự kiện cho Admin
    public List<EventDetailResponseDto> getAllEventsForAdmin() {
        List<Event> events = eventRepository.findAllEvents();
        return events.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết sự kiện cho Admin
    public EventDetailResponseDto getEventDetailForAdmin(Integer eventId) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        return convertToDetailDto(event);
    }

    // Cập nhật sự kiện cho Admin (pending)
    @Transactional
    public EventDetailResponseDto updateEventForAdmin(Integer eventId, EventUpdateRequestDto requestDto) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));

        updateEventFields(event, requestDto);
        // Cập nhật trạng thái nếu có trong yêu cầu
        if (requestDto.getStatus() != null) {
            if (isValidStatus(requestDto.getStatus())) {
                event.setStatus(requestDto.getStatus());
            } else {
                throw new IllegalArgumentException("Trạng thái không hợp lệ");
            }
        }
        eventRepository.save(event);
        return convertToDetailDto(event);
    }
    // Kiểm tra trạng thái hợp lệ
    private boolean isValidStatus(String status) {
        return "pending".equals(status) || "approved".equals(status) || "rejected".equals(status);
    }

    // Xóa sự kiện cho Admin (pending)
    @Transactional
    public void deleteEventForAdmin(Integer eventId) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        if (!"pending".equals(event.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xóa sự kiện ở trạng thái pending");
        }
        // Xóa các areas liên kết trước
        areaRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    // Phê duyệt sự kiện
    @Transactional
    public EventDetailResponseDto approveEvent(Integer eventId) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setStatus("approved");
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return convertToDetailDto(event);
    }

    // Từ chối sự kiện
    @Transactional
    public EventDetailResponseDto rejectEvent(Integer eventId) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setStatus("rejected");
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return convertToDetailDto(event);
    }

    // Phương thức hỗ trợ cập nhật sự kiện cho organizer va admin
    private void updateEventFields(Event event, EventUpdateRequestDto requestDto) {
        if (requestDto.getName() != null) event.setName(requestDto.getName());
        if (requestDto.getDescription() != null) event.setDescription(requestDto.getDescription());
        if (requestDto.getDate() != null) event.setDate(requestDto.getDate());
        if (requestDto.getTime() != null) event.setTime(requestDto.getTime());
        if (requestDto.getLocation() != null) event.setLocation(requestDto.getLocation());
        if (requestDto.getImageUrl() != null) event.setImageUrl(requestDto.getImageUrl());
        event.setUpdatedAt(LocalDateTime.now());
    }

    // Chuyển đổi Event sang EventDetailResponseDto
    private EventDetailResponseDto convertToDetailDto(Event event) {
        EventDetailResponseDto dto = new EventDetailResponseDto();
        dto.setEventId(event.getEventId());
        dto.setCategoryId(event.getCategoryId());
        dto.setOrganizerId(event.getOrganizerId());
        dto.setMapTemplateId(event.getMapTemplateId());
        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setDate(event.getDate());
        dto.setTime(event.getTime());
        dto.setLocation(event.getLocation());
        dto.setImageUrl(event.getImageUrl());
        dto.setStatus(event.getStatus());
        return dto;
    }

    //Chuyển đổi Category sang CategoryPublicListDto
    private CategoryPublicListDto convertToPublicCategoryListDto(Category category) {
        CategoryPublicListDto dto = new CategoryPublicListDto();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }

    // Chuyển đổi Event sang EventPublicListDto
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

    // Chuyển đổi Event sang EventPublicDetailDto
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
}