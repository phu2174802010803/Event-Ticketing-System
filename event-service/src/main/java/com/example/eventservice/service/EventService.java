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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
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

    @Autowired
    private AzureBlobStorageService azureBlobStorageService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    public MapDetailDto getEventMap(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId;
        if (redisTemplate.opsForZSet().score(activeKey, userId.toString()) == null) {
            throw new RuntimeException("Bạn cần tham gia hàng đợi trước khi xem map khu vực");
        }

        Event event = eventRepository.findPublicEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tồn tại hoặc chưa được phê duyệt"));

        List<TemplateArea> templateAreas = templateAreaRepository.findByMapTemplateTemplateId(event.getMapTemplateId());
        if (templateAreas.isEmpty()) {
            throw new RuntimeException("Không tìm thấy template map");
        }

        Integer mapWidth = templateAreas.get(0).getMapTemplate().getMapWidth();
        Integer mapHeight = templateAreas.get(0).getMapTemplate().getMapHeight();

        List<Area> areas = areaRepository.findAll().stream()
                .filter(area -> area.getEventId().equals(eventId))
                .collect(Collectors.toList());

        List<AreaDetailDto> areaDtos = areas.stream()
                .map(area -> {
                    TemplateArea templateArea = templateAreaRepository.findById(area.getTemplateAreaId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy template area"));
                    AreaDetailDto dto = new AreaDetailDto();
                    dto.setAreaId(area.getAreaId());
                    dto.setName(area.getName());
                    dto.setX(templateArea.getX());
                    dto.setY(templateArea.getY());
                    dto.setWidth(templateArea.getWidth());
                    dto.setHeight(templateArea.getHeight());
                    dto.setTotalTickets(area.getTotalTickets());
                    dto.setAvailableTickets(area.getAvailableTickets());
                    dto.setPrice(area.getPrice());
                    return dto;
                })
                .collect(Collectors.toList());

        MapDetailDto mapDetail = new MapDetailDto();
        mapDetail.setMapWidth(mapWidth);
        mapDetail.setMapHeight(mapHeight);
        mapDetail.setAreas(areaDtos);
        return mapDetail;
    }

    public AreaDetailDto getAreaDetail(Integer eventId, Integer areaId) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
        if (!area.getEventId().equals(eventId)) {
            throw new RuntimeException("Khu vực không thuộc sự kiện này");
        }

        AreaDetailDto dto = new AreaDetailDto();
        dto.setAreaId(area.getAreaId());
        dto.setName(area.getName());
        dto.setTotalTickets(area.getTotalTickets());
        dto.setAvailableTickets(area.getAvailableTickets());
        dto.setPrice(area.getPrice());
        return dto;
    }

    @Cacheable(value = "publicEvents", key = "'all'")
    public List<EventPublicListDto> getPublicEvents() {
        List<Event> events = eventRepository.findAllPublicEvents();
        return events.stream()
                .map(this::convertToPublicListDto)
                .collect(Collectors.toList());
    }

    public List<CategoryPublicListDto> getPublicCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::convertToPublicCategoryListDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "publicEventDetail", key = "#eventId")
    public EventPublicDetailDto getPublicEventDetail(Integer eventId) {
        Event event = eventRepository.findPublicEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc sự kiện chưa được phê duyệt"));
        return convertToPublicDetailDto(event);
    }

    public Event getEventById(Integer eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
    }

    @Cacheable(value = "publicEvents", key = "'category:' + #categoryId")
    public List<EventPublicListDto> getPublicEventsByCategory(Integer categoryId) {
        List<Event> events = eventRepository.findPublicEventsByCategory(categoryId);
        return events.stream()
                .map(this::convertToPublicListDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "publicEvents", key = "'featured'")
    public List<EventPublicListDto> getFeaturedPublicEvents() {
        List<Event> events = eventRepository.findFeaturedPublicEvents();
        return events.stream()
                .map(this::convertToPublicListDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "publicEvents", key = "'search:' + #keyword")
    public List<EventPublicListDto> searchPublicEvents(String keyword) {
        List<Event> events = eventRepository.searchPublicEvents(keyword);
        return events.stream()
                .map(this::convertToPublicListDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "bannerEvents")
    public List<EventPublicDetailDto> getBannerEvents() {
        List<Event> events = eventRepository.findTop4EventsWithBanner();
        return events.stream()
                .map(this::convertToPublicDetailDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "publicEvents", key = "'filter:' + #categoryId + ':' + #dateFrom + ':' + #dateTo + ':' + #location")
    public List<EventPublicListDto> filterPublicEvents(Integer categoryId, LocalDate dateFrom, LocalDate dateTo, String location) {
        List<Event> events;
        if (dateFrom != null && dateTo != null) {
            events = eventRepository.findByCategoryIdAndDateRangeAndLocation(categoryId, dateFrom, dateTo, location);
        } else {
            events = eventRepository.findByCategoryIdAndLocation(categoryId, location);
        }
        return events.stream()
                .map(this::convertToPublicListDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Event createEventForOrganizer(EventRequestDto requestDto) {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer organizerId = Integer.parseInt(userIdStr);

        if (requestDto.getCategoryId() != null && !categoryService.categoryExists(requestDto.getCategoryId())) {
            throw new IllegalArgumentException("Danh mục không tồn tại");
        }
        if (eventRepository.existsByEventNameAndOrganizerId(requestDto.getName(), organizerId)) {
            throw new IllegalArgumentException("Sự kiện đã tồn tại");
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

        List<TemplateArea> templateAreas = templateAreaRepository.findByMapTemplateTemplateId(requestDto.getMapTemplateId());
        if (templateAreas.isEmpty()) {
            throw new IllegalArgumentException("Template map không có khu vực nào");
        }

        if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
            for (var areaDto : requestDto.getAreas()) {
                TemplateArea matchingTemplateArea = templateAreas.stream()
                        .filter(ta -> ta.getTemplateAreaId().equals(areaDto.getTemplateAreaId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("template_area_id không hợp lệ: " + areaDto.getTemplateAreaId()));
                Area area = new Area();
                area.setEventId(savedEvent.getEventId());
                area.setTemplateAreaId(matchingTemplateArea.getTemplateAreaId());
                area.setName(areaDto.getName() != null ? areaDto.getName() : matchingTemplateArea.getName());
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
        if (requestDto.getCategoryId() != null && !categoryService.categoryExists(requestDto.getCategoryId())) {
            throw new IllegalArgumentException("Danh mục không tồn tại");
        }
        if (eventRepository.existsByEventNameAndOrganizerId(requestDto.getName(), organizerId)) {
            throw new IllegalArgumentException("Sự kiện đã tồn tại");
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

        List<TemplateArea> templateAreas = templateAreaRepository.findByMapTemplateTemplateId(requestDto.getMapTemplateId());
        if (templateAreas.isEmpty()) {
            throw new IllegalArgumentException("Template map không có khu vực nào");
        }

        if (requestDto.getAreas() != null && !requestDto.getAreas().isEmpty()) {
            for (var areaDto : requestDto.getAreas()) {
                TemplateArea matchingTemplateArea = templateAreas.stream()
                        .filter(ta -> ta.getTemplateAreaId().equals(areaDto.getTemplateAreaId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("template_area_id không hợp lệ: " + areaDto.getTemplateAreaId()));
                Area area = new Area();
                area.setEventId(savedEvent.getEventId());
                area.setTemplateAreaId(matchingTemplateArea.getTemplateAreaId());
                area.setName(areaDto.getName() != null ? areaDto.getName() : matchingTemplateArea.getName());
                area.setTotalTickets(areaDto.getTotalTickets());
                area.setAvailableTickets(areaDto.getTotalTickets());
                area.setPrice(areaDto.getPrice());
                areaRepository.save(area);

            }
        }
        return savedEvent;
    }

    public List<EventDetailResponseDto> getEventsByOrganizer(Integer organizerId) {
        List<Event> events = eventRepository.findByOrganizerId(organizerId);
        return events.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    public EventDetailResponseDto getEventDetailForOrganizer(Integer eventId, Integer organizerId) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        return convertToDetailDto(event);
    }

    @Transactional
    public EventDetailResponseDto updateEventForOrganizer(Integer eventId, Integer organizerId, EventUpdateRequestDto requestDto) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setStatus("pending");
        updateEventFields(event, requestDto);
        eventRepository.save(event);
        return convertToDetailDto(event);
    }

    @Transactional
    public void deleteEventForOrganizer(Integer eventId, Integer organizerId) {
        Event event = eventRepository.findByEventIdAndOrganizerId(eventId, organizerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        if (!"pending".equals(event.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xóa sự kiện ở trạng thái pending");
        }
        azureBlobStorageService.deleteImage(event.getImageUrl());
        azureBlobStorageService.deleteImage(event.getBannerUrl());
        areaRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    public List<EventDetailResponseDto> getAllEventsForAdmin() {
        List<Event> events = eventRepository.findAllEvents();
        return events.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    public EventDetailResponseDto getEventDetailForAdmin(Integer eventId) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        return convertToDetailDto(event);
    }

    @Transactional
    public EventDetailResponseDto updateEventForAdmin(Integer eventId, EventUpdateRequestDto requestDto) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        updateEventFields(event, requestDto);
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

    private boolean isValidStatus(String status) {
        return "pending".equals(status) || "approved".equals(status) || "rejected".equals(status);
    }

    @Transactional
    public void deleteEventForAdmin(Integer eventId) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        if (!"pending".equals(event.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xóa sự kiện ở trạng thái pending");
        }
        azureBlobStorageService.deleteImage(event.getImageUrl());
        azureBlobStorageService.deleteImage(event.getBannerUrl());
        areaRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    @Transactional
    public EventDetailResponseDto approveEvent(Integer eventId) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setStatus("approved");
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return convertToDetailDto(event);
    }

    @Transactional
    public EventDetailResponseDto rejectEvent(Integer eventId) {
        Event event = eventRepository.findEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setStatus("rejected");
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);
        return convertToDetailDto(event);
    }

    private void updateEventFields(Event event, EventUpdateRequestDto requestDto) {
        if (requestDto.getName() != null) event.setName(requestDto.getName());
        if (requestDto.getDescription() != null) event.setDescription(requestDto.getDescription());
        if (requestDto.getDate() != null) event.setDate(requestDto.getDate());
        if (requestDto.getTime() != null) event.setTime(requestDto.getTime());
        if (requestDto.getLocation() != null) event.setLocation(requestDto.getLocation());
        event.setImageUrl(requestDto.getImageUrl());
        event.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void updateEventImageUrl(Integer eventId, String imageUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setImageUrl(imageUrl);
        eventRepository.save(event);
    }

    @Transactional
    public void updateEventBannerUrl(Integer eventId, String bannerUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setBannerUrl(bannerUrl);
        eventRepository.save(event);
    }

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
        dto.setBannerUrl(event.getBannerUrl());
        dto.setStatus(event.getStatus());
        return dto;
    }

    private CategoryPublicListDto convertToPublicCategoryListDto(Category category) {
        CategoryPublicListDto dto = new CategoryPublicListDto();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        return dto;
    }

    private EventPublicListDto convertToPublicListDto(Event event) {
        EventPublicListDto dto = new EventPublicListDto();
        dto.setEventId(event.getEventId());
        dto.setName(event.getName());
        dto.setDate(event.getDate());
        dto.setTime(event.getTime());
        dto.setLocation(event.getLocation());
        dto.setImageUrl(event.getImageUrl());
        dto.setBannerUrl(event.getBannerUrl());
        return dto;
    }

    private EventPublicDetailDto convertToPublicDetailDto(Event event) {
        EventPublicDetailDto dto = new EventPublicDetailDto();
        dto.setEventId(event.getEventId());
        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setDate(event.getDate());
        dto.setTime(event.getTime());
        dto.setLocation(event.getLocation());
        dto.setImageUrl(event.getImageUrl());
        dto.setBannerUrl(event.getBannerUrl());
        dto.setCategory(categoryService.getCategoryById(event.getCategoryId()));
        return dto;
    }
}