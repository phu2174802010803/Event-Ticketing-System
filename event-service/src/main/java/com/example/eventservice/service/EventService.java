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
                    return new SellingPhaseResponseDto(
                            phase.getPhaseId(),
                            phase.getEventId(),
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
                    return new SellingPhaseResponseDto(
                            phase.getPhaseId(),
                            phase.getEventId(),
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
                    return new SellingPhaseResponseDto(
                            phase.getPhaseId(),
                            phase.getEventId(),
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

        //Kiểm tra cập nhật thời gian không trong quá khứ
        if (requestDto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được trong quá khứ");
        }
        if (requestDto.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian kết thúc không được trong quá khứ");
        }

        // Kiểm tra thời gian hợp lệ
        if (requestDto.getStartTime().isAfter(requestDto.getEndTime())) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        // Kiểm tra trùng lặp thời gian với các phiên khác
        List<SellingPhase> existingPhases = sellingPhaseRepository.findByEventId(eventId);
        for (SellingPhase phase : existingPhases) {
            if (requestDto.getStartTime().isBefore(phase.getEndTime()) &&
                    requestDto.getEndTime().isAfter(phase.getStartTime())) {
                throw new IllegalArgumentException("Thời gian phiên bán vé trùng với phiên khác");
            }
        }

        // Tính tổng availableTickets và totalTickets của tất cả khu vực trong sự kiện
        List<Area> areas = areaRepository.findAll().stream()
                .filter(area -> area.getEventId().equals(eventId))
                .collect(Collectors.toList());
        int totalAvailableTickets = areas.stream().mapToInt(Area::getAvailableTickets).sum();
        int totalTickets = areas.stream().mapToInt(Area::getTotalTickets).sum();

        // Kiểm tra tickets_available không vượt quá totalTickets hoặc availableTickets
        if (requestDto.getTicketsAvailable() > totalTickets) {
            throw new IllegalArgumentException("Số vé của phiên bán (" + requestDto.getTicketsAvailable() +
                    ") vượt quá tổng số vé của sự kiện (" + totalTickets + ")");
        }
        if (requestDto.getTicketsAvailable() > totalAvailableTickets) {
            throw new IllegalArgumentException("Số vé của phiên bán (" + requestDto.getTicketsAvailable() +
                    ") vượt quá số vé còn lại của sự kiện (" + totalAvailableTickets + ")");
        }

        // Kiểm tra tổng tickets_available của các phiên bán không vượt quá totalTickets hoặc availableTickets
        int totalPhaseTickets = existingPhases.stream().mapToInt(SellingPhase::getTicketsAvailable).sum();
        int newTotalPhaseTickets = totalPhaseTickets + requestDto.getTicketsAvailable();

        if (newTotalPhaseTickets > totalTickets) {
            throw new IllegalArgumentException("Tổng số vé của các phiên bán vượt quá tổng số vé của sự kiện");
        }
        if (newTotalPhaseTickets > totalAvailableTickets) {
            throw new IllegalArgumentException("Tổng số vé của các phiên bán vượt quá số vé còn lại của sự kiện");
        }

        SellingPhase phase = new SellingPhase();
        phase.setEventId(eventId);
        phase.setStartTime(requestDto.getStartTime());
        phase.setEndTime(requestDto.getEndTime());
        phase.setTicketsAvailable(requestDto.getTicketsAvailable());
        SellingPhase savedPhase = sellingPhaseRepository.save(phase);

        // Xác định trạng thái phiên dựa trên thời gian thực
        String status = determinePhaseStatus(savedPhase.getStartTime(), savedPhase.getEndTime());
        redisTemplate.opsForValue().set("phase:status:" + savedPhase.getPhaseId(), status);

        return new SellingPhaseResponseDto(
                savedPhase.getPhaseId(),
                savedPhase.getEventId(),
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

    // Cập nhật phiên bán vé (Organizer và Admin)
    @Transactional
    public SellingPhaseResponseDto updateSellingPhase(Integer phaseId, SellingPhaseRequestDto requestDto) {
        SellingPhase phase = sellingPhaseRepository.findById(phaseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bán vé"));

        Event event = eventRepository.findById(phase.getEventId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));

        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = Integer.parseInt(userIdStr);
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "");

        if (!role.equals("ADMIN") && !event.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật phiên bán vé này");
        }

        // Kiểm tra thời gian hợp lệ
        if (requestDto.getStartTime().isAfter(requestDto.getEndTime())) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        //Kiểm tra cập nhật thời gian không trong quá khứ
        if (requestDto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được trong quá khứ");
        }
        if (requestDto.getEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian kết thúc không được trong quá khứ");
        }

        // Kiểm tra trùng lặp thời gian
        List<SellingPhase> otherPhases = sellingPhaseRepository.findByEventId(phase.getEventId()).stream()
                .filter(p -> !p.getPhaseId().equals(phaseId))
                .collect(Collectors.toList());
        for (SellingPhase p : otherPhases) {
            if (requestDto.getStartTime().isBefore(p.getEndTime()) &&
                    requestDto.getEndTime().isAfter(p.getStartTime())) {
                throw new IllegalArgumentException("Thời gian phiên bán vé trùng với phiên khác");
            }
        }

        // Tính tổng availableTickets và totalTickets của tất cả khu vực trong sự kiện
        List<Area> areas = areaRepository.findAll().stream()
                .filter(area -> area.getEventId().equals(phase.getEventId()))
                .collect(Collectors.toList());
        int totalAvailableTickets = areas.stream().mapToInt(Area::getAvailableTickets).sum();
        int totalTickets = areas.stream().mapToInt(Area::getTotalTickets).sum();

        // Kiểm tra tickets_available không vượt quá totalTickets hoặc availableTickets
        if (requestDto.getTicketsAvailable() > totalTickets) {
            throw new IllegalArgumentException("Số vé của phiên bán (" + requestDto.getTicketsAvailable() +
                    ") vượt quá tổng số vé của sự kiện (" + totalTickets + ")");
        }
        if (requestDto.getTicketsAvailable() > totalAvailableTickets) {
            throw new IllegalArgumentException("Số vé của phiên bán (" + requestDto.getTicketsAvailable() +
                    ") vượt quá số vé còn lại của sự kiện (" + totalAvailableTickets + ")");
        }

        // Tính tổng tickets_available của tất cả các phiên bán vé trừ phiên hiện tại
        int totalOtherPhaseTickets = otherPhases.stream().mapToInt(SellingPhase::getTicketsAvailable).sum();
        int newTotalPhaseTickets = totalOtherPhaseTickets + requestDto.getTicketsAvailable();

        // Kiểm tra tổng mới không vượt quá totalTickets và availableTickets
        if (newTotalPhaseTickets > totalTickets) {
            throw new IllegalArgumentException("Tổng số vé của các phiên bán (" + newTotalPhaseTickets +
                    ") vượt quá tổng số vé của sự kiện (" + totalTickets + ")");
        }
        if (newTotalPhaseTickets > totalAvailableTickets) {
            throw new IllegalArgumentException("Tổng số vé của các phiên bán (" + newTotalPhaseTickets +
                    ") vượt quá số vé còn lại của sự kiện (" + totalAvailableTickets + ")");
        }

        // Cập nhật thông tin phiên bán vé
        phase.setStartTime(requestDto.getStartTime());
        phase.setEndTime(requestDto.getEndTime());
        phase.setTicketsAvailable(requestDto.getTicketsAvailable());
        phase.setUpdatedAt(LocalDateTime.now());
        SellingPhase updatedPhase = sellingPhaseRepository.save(phase);

        String status = determinePhaseStatus(updatedPhase.getStartTime(), updatedPhase.getEndTime());
        redisTemplate.opsForValue().set("phase:status:" + updatedPhase.getPhaseId(), status);

        return new SellingPhaseResponseDto(
                updatedPhase.getPhaseId(),
                updatedPhase.getEventId(),
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
        azureBlobStorageService.deleteImage(event.getImageUrl());
        azureBlobStorageService.deleteImage(event.getBannerUrl());
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