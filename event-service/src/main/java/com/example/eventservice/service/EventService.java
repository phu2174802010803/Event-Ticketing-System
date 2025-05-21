package com.example.eventservice.service;

import com.example.eventservice.dto.*;
import com.example.eventservice.model.*;
import com.example.eventservice.repository.*;
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
    private SellingPhaseRepository sellingPhaseRepository;

    @Autowired
    private SpacesStorageService spacesStorageService;

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

    // Cập nhật chi tiết sự kiện công khai để hiển thị phiên bán vé cho User
    @Cacheable(value = "publicEventDetail", key = "#eventId")
    public EventPublicDetailDto getPublicEventDetail(Integer eventId) {
        Event event = eventRepository.findPublicEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc sự kiện chưa được phê duyệt"));
        EventPublicDetailDto dto = convertToPublicDetailDto(event);

        // Thêm thông tin phiên bán vé
        List<SellingPhase> phases = sellingPhaseRepository.findByEventId(eventId);
        List<SellingPhaseResponseDto> phaseDtos = phases.stream()
                .map(phase -> {
                    String status = determinePhaseStatus(phase.getStartTime(), phase.getEndTime());
                    redisTemplate.opsForValue().set("phase:status:" + phase.getPhaseId(), status);
                    String areaName = null;
                    if (phase.getAreaId() != null) {
                        Area area = areaRepository.findById(phase.getAreaId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
                        areaName = area.getName();
                    }
                    return new SellingPhaseResponseDto(
                            phase.getPhaseId(),
                            phase.getEventId(),
                            phase.getAreaId(),
                            areaName,
                            phase.getStartTime(),
                            phase.getEndTime(),
                            phase.getTicketsAvailable(),
                            status,
                            null
                    );
                })
                .collect(Collectors.toList());
        dto.setSellingPhases(phaseDtos);

        return dto;
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

    // Xem danh sách phiên bán vé công khai (không cần xác thực)
    public List<SellingPhaseResponseDto> getPublicSellingPhasesByEvent(Integer eventId) {
        Event event = eventRepository.findPublicEventById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện hoặc sự kiện chưa được phê duyệt"));

        List<SellingPhase> phases = sellingPhaseRepository.findByEventId(eventId);
        return phases.stream()
                .map(phase -> {
                    String status = determinePhaseStatus(phase.getStartTime(), phase.getEndTime());
                    redisTemplate.opsForValue().set("phase:status:" + phase.getPhaseId(), status);
                    String areaName = null;
                    if (phase.getAreaId() != null) {
                        Area area = areaRepository.findById(phase.getAreaId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
                        areaName = area.getName();
                    }
                    return new SellingPhaseResponseDto(
                            phase.getPhaseId(),
                            phase.getEventId(),
                            phase.getAreaId(),
                            areaName,
                            phase.getStartTime(),
                            phase.getEndTime(),
                            phase.getTicketsAvailable(),
                            status,
                            null
                    );
                })
                .collect(Collectors.toList());
    }

    // Xem danh sách phiên bán vé (Organizer và Admin)
    public List<SellingPhaseResponseDto> getSellingPhasesByEvent(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));

        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = Integer.parseInt(userIdStr);
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "");

        if (!role.equals("ADMIN") && !event.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem phiên bán vé của sự kiện này");
        }

        List<SellingPhase> phases = sellingPhaseRepository.findByEventId(eventId);
        return phases.stream()
                .map(phase -> {
                    String status = determinePhaseStatus(phase.getStartTime(), phase.getEndTime());
                    redisTemplate.opsForValue().set("phase:status:" + phase.getPhaseId(), status);
                    String areaName = null;
                    if (phase.getAreaId() != null) {
                        Area area = areaRepository.findById(phase.getAreaId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
                        areaName = area.getName();
                    }
                    return new SellingPhaseResponseDto(
                            phase.getPhaseId(),
                            phase.getEventId(),
                            phase.getAreaId(),
                            areaName,
                            phase.getStartTime(),
                            phase.getEndTime(),
                            phase.getTicketsAvailable(),
                            status,
                            null
                    );
                })
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

    // Tạo phiên bán vé (Organizer và Admin)
    @Transactional
    public SellingPhaseResponseDto createSellingPhase(Integer eventId, SellingPhaseRequestDto requestDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));

        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = Integer.parseInt(userIdStr);
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "");

        if (!role.equals("ADMIN") && !event.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền tạo phiên bán vé cho sự kiện này");
        }

        // Kiểm tra thời gian hợp lệ
        if (requestDto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được trong quá khứ");
        }
        if (requestDto.getEndTime().isBefore(requestDto.getStartTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        // Kiểm tra thời gian trùng lặp chỉ với các phiên của cùng khu vực
        if (requestDto.getAreaId() != null) {
            List<SellingPhase> existingPhases = sellingPhaseRepository.findByEventIdAndAreaId(eventId, requestDto.getAreaId());
            for (SellingPhase phase : existingPhases) {
                if (requestDto.getStartTime().isBefore(phase.getEndTime()) &&
                        requestDto.getEndTime().isAfter(phase.getStartTime())) {
                    throw new IllegalArgumentException("Thời gian phiên bán vé trùng với phiên khác trong cùng khu vực");
                }
            }
        }

        // Kiểm tra tổng số vé khả dụng của các phiên bán vé cho khu vực
        if (requestDto.getAreaId() != null) {
            Area area = areaRepository.findById(requestDto.getAreaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
            if (!area.getEventId().equals(eventId)) {
                throw new IllegalArgumentException("Khu vực không thuộc sự kiện này");
            }
            List<SellingPhase> phasesForArea = sellingPhaseRepository.findByEventIdAndAreaId(eventId, requestDto.getAreaId());
            int totalAllocatedTickets = phasesForArea.stream().mapToInt(SellingPhase::getTicketsAvailable).sum();
            if (totalAllocatedTickets + requestDto.getTicketsAvailable() > area.getTotalTickets()) {
                throw new IllegalArgumentException("Tổng số vé của các phiên bán vượt quá số vé của khu vực");
            }
        } else {
            List<Area> areas = areaRepository.findAll().stream()
                    .filter(area -> area.getEventId().equals(eventId))
                    .collect(Collectors.toList());
            int totalAvailableTickets = areas.stream().mapToInt(Area::getTotalTickets).sum();
            List<SellingPhase> phasesForEvent = sellingPhaseRepository.findByEventId(eventId);
            int totalAllocatedTickets = phasesForEvent.stream().mapToInt(SellingPhase::getTicketsAvailable).sum();
            if (totalAllocatedTickets + requestDto.getTicketsAvailable() > totalAvailableTickets) {
                throw new IllegalArgumentException("Tổng số vé của các phiên bán vượt quá số vé của sự kiện");
            }
        }

        SellingPhase phase = new SellingPhase();
        phase.setEventId(eventId);
        phase.setAreaId(requestDto.getAreaId());
        phase.setStartTime(requestDto.getStartTime());
        phase.setEndTime(requestDto.getEndTime());
        phase.setTicketsAvailable(requestDto.getTicketsAvailable());
        SellingPhase savedPhase = sellingPhaseRepository.save(phase);

        // Xác định trạng thái phiên dựa trên thời gian thực
        String status = determinePhaseStatus(savedPhase.getStartTime(), savedPhase.getEndTime());
        redisTemplate.opsForValue().set("phase:status:" + savedPhase.getPhaseId(), status);
        String areaName = null;
        if (savedPhase.getAreaId() != null) {
            Area area = areaRepository.findById(savedPhase.getAreaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
            areaName = area.getName();
        }

        return new SellingPhaseResponseDto(
                savedPhase.getPhaseId(),
                savedPhase.getEventId(),
                savedPhase.getAreaId(),
                areaName,
                savedPhase.getStartTime(),
                savedPhase.getEndTime(),
                savedPhase.getTicketsAvailable(),
                status,
                "Tạo phiên bán vé thành công"
        );
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
        spacesStorageService.deleteImage(event.getImageUrl());
        spacesStorageService.deleteImage(event.getBannerUrl());
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

    // Cập nhật phiên bán vé (Organizer và Admin)
    @Transactional
    public SellingPhaseResponseDto updateSellingPhase(Integer phaseId, SellingPhaseRequestDto requestDto) {
        //Tìm phiên bán vé theo phaseId
        SellingPhase phase = sellingPhaseRepository.findById(phaseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bán vé"));

        Event event = eventRepository.findById(phase.getEventId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));

        //Kiểm tra quyền truy cập
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = Integer.parseInt(userIdStr);
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "");

        if (!role.equals("ADMIN") && !event.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật phiên bán vé này");
        }

        // Kiểm tra thời gian trùng lặp khi cập nhật chỉ với các phiên của cùng khu vực
        if (requestDto.getAreaId() != null) {
            List<SellingPhase> otherPhases = sellingPhaseRepository.findByEventIdAndAreaId(phase.getEventId(), requestDto.getAreaId()).stream()
                    .filter(p -> !p.getPhaseId().equals(phaseId))
                    .collect(Collectors.toList());
            for (SellingPhase p : otherPhases) {
                if (requestDto.getStartTime().isBefore(p.getEndTime()) &&
                        requestDto.getEndTime().isAfter(p.getStartTime())) {
                    throw new IllegalArgumentException("Thời gian phiên bán vé trùng với phiên khác trong cùng khu vực");
                }
            }
        }

        // Kiểm tra tổng số vé khả dụng khi cập nhật
        if (requestDto.getAreaId() != null) {
            Area area = areaRepository.findById(requestDto.getAreaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
            if (!area.getEventId().equals(phase.getEventId())) {
                throw new IllegalArgumentException("Khu vực không thuộc sự kiện này");
            }
            List<SellingPhase> otherPhases = sellingPhaseRepository.findByEventIdAndAreaId(phase.getEventId(), requestDto.getAreaId()).stream()
                    .filter(p -> !p.getPhaseId().equals(phaseId))
                    .collect(Collectors.toList());
            int totalAllocatedTickets = otherPhases.stream().mapToInt(SellingPhase::getTicketsAvailable).sum();
            if (totalAllocatedTickets + requestDto.getTicketsAvailable() > area.getTotalTickets()) {
                throw new IllegalArgumentException("Tổng số vé của các phiên bán vượt quá số vé của khu vực");
            }
            phase.setAreaId(requestDto.getAreaId());
        } else {
            phase.setAreaId(null);
            List<Area> areas = areaRepository.findAll().stream()
                    .filter(area -> area.getEventId().equals(phase.getEventId()))
                    .collect(Collectors.toList());
            int totalAvailableTickets = areas.stream().mapToInt(Area::getTotalTickets).sum();
            List<SellingPhase> otherPhases = sellingPhaseRepository.findByEventId(phase.getEventId()).stream()
                    .filter(p -> !p.getPhaseId().equals(phaseId))
                    .collect(Collectors.toList());
            int totalAllocatedTickets = otherPhases.stream().mapToInt(SellingPhase::getTicketsAvailable).sum();
            if (totalAllocatedTickets + requestDto.getTicketsAvailable() > totalAvailableTickets) {
                throw new IllegalArgumentException("Tổng số vé của các phiên bán vượt quá số vé của sự kiện");
            }
        }

        // Cập nhật các trường khác
        phase.setStartTime(requestDto.getStartTime());
        phase.setEndTime(requestDto.getEndTime());
        phase.setTicketsAvailable(requestDto.getTicketsAvailable());
        phase.setUpdatedAt(LocalDateTime.now());

        // Lưu thay đổi
        SellingPhase updatedPhase = sellingPhaseRepository.save(phase);

        // Xác định trạng thái và lấy tên khu vực
        String status = determinePhaseStatus(updatedPhase.getStartTime(), updatedPhase.getEndTime());
        String areaName = null;
        if (updatedPhase.getAreaId() != null) {
            Area area = areaRepository.findById(updatedPhase.getAreaId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực"));
            areaName = area.getName();
        }

        // Trả về DTO với thông tin đầy đủ
        return new SellingPhaseResponseDto(
                updatedPhase.getPhaseId(),
                updatedPhase.getEventId(),
                updatedPhase.getAreaId(),
                areaName,
                updatedPhase.getStartTime(),
                updatedPhase.getEndTime(),
                updatedPhase.getTicketsAvailable(),
                status,
                "Cập nhật phiên bán vé thành công"
        );
    }

    // Xác định trạng thái phiên bán vé dựa trên thời gian
    private String determinePhaseStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return "upcoming";
        } else if (now.isAfter(endTime)) {
            return "ended";
        } else {
            return "active";
        }
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
        spacesStorageService.deleteImage(event.getImageUrl());
        spacesStorageService.deleteImage(event.getBannerUrl());
        areaRepository.deleteByEventId(eventId);
        eventRepository.delete(event);
    }

    // Xóa phiên bán vé (Organizer và Admin)
    @Transactional
    public void deleteSellingPhase(Integer phaseId) {
        SellingPhase phase = sellingPhaseRepository.findById(phaseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bán vé"));

        Event event = eventRepository.findById(phase.getEventId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));

        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = Integer.parseInt(userIdStr);
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "");

        if (!role.equals("ADMIN") && !event.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa phiên bán vé này");
        }

        sellingPhaseRepository.delete(phase);
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