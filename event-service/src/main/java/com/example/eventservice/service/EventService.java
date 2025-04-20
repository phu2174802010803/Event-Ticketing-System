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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

    private static final int MAX_ACTIVE_USERS = 2; // Giới hạn 2 người dùng đồng thời
    private static final int ACCESS_TIME_MINUTES = 2; // TTL cho người dùng hoạt động
    private static final int CONFIRMATION_INTERVAL_MINUTES = 5; // TTL cho người dùng trong hàng đợi

    // Tham gia hàng đợi
    public String joinQueue(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId; // Sorted Set cho người dùng hoạt động
        String queueKey = "queue:" + eventId;   // List cho hàng đợi
        String userKey = "user:" + eventId + ":" + userId; // Trạng thái người dùng
        String userActiveKey = "user_active:" + eventId + ":" + userId; // Khóa TTL riêng

        // Kiểm tra xem người dùng đã ở trong activeKey chưa
        Double score = redisTemplate.opsForZSet().score(activeKey, userId.toString());
        if (score != null) {
            // Người dùng đã ở trong activeKey, lấy thứ hạng và thông báo
            Long rank = redisTemplate.opsForZSet().rank(activeKey, userId.toString());
            return "Bạn đã ở trong danh sách hoạt động, vị trí thứ " + (rank != null ? rank + 1 : "không xác định");
        }

        // Kiểm tra số lượng người dùng trong activeKey
        Long activeCount = redisTemplate.opsForZSet().size(activeKey);
        if (activeCount != null && activeCount < MAX_ACTIVE_USERS) {
            // Thêm vào danh sách hoạt động
            long timestamp = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(activeKey, userId.toString(), timestamp);
            redisTemplate.opsForValue().set(userActiveKey, "active", ACCESS_TIME_MINUTES, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(userKey, "active");
            return "Bạn có thể truy cập ngay lập tức";
        } else {
            // Thêm vào hàng đợi
            Long position = redisTemplate.opsForList().rightPush(queueKey, userId.toString());
            redisTemplate.opsForValue().set(userKey, "queue", CONFIRMATION_INTERVAL_MINUTES, TimeUnit.MINUTES);
            return "Bạn đang ở vị trí thứ " + position + " trong hàng đợi";
        }
    }

    // Kiểm tra trạng thái hàng đợi
    public String checkQueueStatus(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId;
        String queueKey = "queue:" + eventId;
        String userKey = "user:" + eventId + ":" + userId;

        String status = redisTemplate.opsForValue().get(userKey);
        if ("active".equals(status)) {
            Long rank = redisTemplate.opsForZSet().rank(activeKey, userId.toString());
            if (rank != null) {
                return "Bạn đang ở vị trí thứ " + (rank + 1) + " trong danh sách hoạt động";
            } else {
                return "Bạn không ở trong danh sách hoạt động";
            }
        } else if ("queue".equals(status)) {
            Long position = redisTemplate.opsForList().indexOf(queueKey, userId.toString());
            if (position != null && position >= 0) {
                return "Bạn đang ở vị trí thứ " + (position + 1) + " trong hàng đợi";
            }
        }
        return "Bạn không ở trong hàng đợi";
    }

    // Xác nhận tiếp tục trong hàng đợi
    public void confirmPresence(Integer eventId, Integer userId) {
        String userKey = "user:" + eventId + ":" + userId;
        String status = redisTemplate.opsForValue().get(userKey);
        if ("queue".equals(status)) {
            redisTemplate.expire(userKey, CONFIRMATION_INTERVAL_MINUTES, TimeUnit.MINUTES);
        }
    }

    // Rời hàng đợi
    public void leaveQueue(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId;
        String queueKey = "queue:" + eventId;
        String userKey = "user:" + eventId + ":" + userId;
        String userActiveKey = "user_active:" + eventId + ":" + userId;

        redisTemplate.opsForZSet().remove(activeKey, userId.toString());
        redisTemplate.opsForList().remove(queueKey, 0, userId.toString());
        redisTemplate.delete(userKey);
        redisTemplate.delete(userActiveKey);
    }

    // Xử lý khi người dùng hết hạn (được gọi từ Redis Pub/Sub)
    public void handleUserExpired(String message) {
        if (message.startsWith("user_active:")) {
            String[] parts = message.split(":");
            Integer eventId = Integer.parseInt(parts[1]);
            Integer userId = Integer.parseInt(parts[2]);
            removeUserFromActive(eventId, userId);
            moveFromQueueToActive(eventId);
        }
    }

    // Xóa người dùng khỏi danh sách hoạt động
    private void removeUserFromActive(Integer eventId, Integer userId) {
        String activeKey = "active:" + eventId;
        String userKey = "user:" + eventId + ":" + userId;
        redisTemplate.opsForZSet().remove(activeKey, userId.toString());
        redisTemplate.delete(userKey);
    }

    // Chuyển người dùng từ hàng đợi lên danh sách hoạt động
    private void moveFromQueueToActive(Integer eventId) {
        String activeKey = "active:" + eventId;
        String queueKey = "queue:" + eventId;
        Long activeCount = redisTemplate.opsForZSet().size(activeKey);

        while (activeCount != null && activeCount < MAX_ACTIVE_USERS && redisTemplate.opsForList().size(queueKey) > 0) {
            String nextUser = redisTemplate.opsForList().leftPop(queueKey);
            if (nextUser != null) {
                Integer userId = Integer.parseInt(nextUser);
                String userKey = "user:" + eventId + ":" + userId;
                String userActiveKey = "user_active:" + eventId + ":" + userId;
                long timestamp = System.currentTimeMillis();
                redisTemplate.opsForZSet().add(activeKey, nextUser, timestamp);
                redisTemplate.opsForValue().set(userKey, "active");
                redisTemplate.opsForValue().set(userActiveKey, "active", ACCESS_TIME_MINUTES, TimeUnit.MINUTES);
                activeCount = redisTemplate.opsForZSet().size(activeKey);
            }
        }
    }

    // Các phương thức khác giữ nguyên từ mã cũ của bạn
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

    // Lấy chi tiết một sự kiện theo ID
    public Event getEventById(Integer eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
    }

    // Lấy sự kiện theo danh mục
    public List<EventPublicListDto> getPublicEventsByCategory(Integer categoryId) {
        List<Event> events = eventRepository.findPublicEventsByCategory(categoryId);
        return events.stream()
                .map(this::convertToPublicListDto)
                .collect(Collectors.toList());
    }

    // Lấy sự kiện nổi bật
    public List<EventPublicListDto> getFeaturedPublicEvents() {
        List<Event> events = eventRepository.findFeaturedPublicEvents();
        return events.stream()
                .map(this::convertToPublicListDto)
                .collect(Collectors.toList());
    }

    // Tìm kiếm sự kiện công khai theo tên hoặc địa điểm
    public List<EventPublicListDto> searchPublicEvents(String keyword) {
        List<Event> events = eventRepository.searchPublicEvents(keyword);
        return events.stream()
                .map(this::convertToPublicListDto)
                .collect(Collectors.toList());
    }

    // Thêm phương thức lấy sự kiện có banner cho trang chủ
    public List<EventPublicDetailDto> getBannerEvents() {
        List<Event> events = eventRepository.findTop4EventsWithBanner();
        return events.stream()
                .map(this::convertToPublicDetailDto)
                .collect(Collectors.toList());
    }

    // Thêm phương thức lọc sự kiện công khai
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

        //Kiểm tra category_id
        if (requestDto.getCategoryId() != null && !categoryService.categoryExists(requestDto.getCategoryId())) {
            throw new IllegalArgumentException("Danh mục không tồn tại");
        }
        //Kiểm tra sự kiện đã tồn tại hay chưa
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
        //Kiểm tra sự kiện đã tồn tại hay chưa
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
        azureBlobStorageService.deleteImage(event.getImageUrl());
        azureBlobStorageService.deleteImage(event.getBannerUrl());
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
        azureBlobStorageService.deleteImage(event.getImageUrl());
        azureBlobStorageService.deleteImage(event.getBannerUrl());
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
        event.setImageUrl(requestDto.getImageUrl());
        event.setUpdatedAt(LocalDateTime.now());
    }

    // Cập nhật URL hình ảnh sự kiện
    @Transactional
    public void updateEventImageUrl(Integer eventId, String imageUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setImageUrl(imageUrl);
        eventRepository.save(event);
    }

    // Thêm phương thức update banner URL
    @Transactional
    public void updateEventBannerUrl(Integer eventId, String bannerUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự kiện"));
        event.setBannerUrl(bannerUrl);
        eventRepository.save(event);
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
        dto.setBannerUrl(event.getBannerUrl());
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
        dto.setBannerUrl(event.getBannerUrl());
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
        dto.setBannerUrl(event.getBannerUrl());
        dto.setCategory(categoryService.getCategoryById(event.getCategoryId()));
        return dto;
    }
}