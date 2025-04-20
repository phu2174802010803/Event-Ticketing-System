package com.example.eventservice.repository;

import com.example.eventservice.model.Category;
import com.example.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer> {

    //Lấy danh sách sự kiện của Organizer
    @Query("SELECT e FROM Event e WHERE e.organizerId = :organizerId")
    List<Event> findByOrganizerId (@Param("organizerId") Integer organizerId);

    //Lấy sự kiện theo ID và Organizer
    @Query("SELECT e FROM Event e WHERE e.eventId = :eventId AND e.organizerId = :organizerId")
    Optional<Event> findByEventIdAndOrganizerId(@Param("eventId") Integer eventId,
                                                @Param("organizerId") Integer organizerId);

    // Lấy tất cả sự kiện cho Admin
    @Query("SELECT e FROM Event e")
    List<Event> findAllEvents();

    // Lấy sự kiện theo ID cho Admin
    @Query("SELECT e FROM Event e WHERE e.eventId = :eventId")
    Optional<Event> findEventById(@Param("eventId") Integer eventId);

    // Lấy danh sách tất cả sự kiện công khai (status = 'approved')
    @Query("SELECT e FROM Event e WHERE e.status = 'approved'")
    List<Event> findAllPublicEvents();

    // Lấy chi tiết một sự kiện công khai theo ID
    @Query("SELECT e FROM Event e WHERE e.eventId = :eventId AND e.status = 'approved'")
    Optional<Event> findPublicEventById(@Param("eventId") Integer eventId);

    // Kiểm tra tên sự kiện theo organizerId đã tồn tại
    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE e.name = :name AND e.organizerId = :organizerId")
    boolean existsByEventNameAndOrganizerId(@Param("name") String name,
                                            @Param("organizerId") Integer organizerId);

    // Lấy sự kiện theo danh mục
    @Query("SELECT e FROM Event e WHERE e.categoryId = :categoryId AND e.status = 'approved'")
    List<Event> findPublicEventsByCategory(@Param("categoryId") Integer categoryId);

    // Lấy sự kiện nổi bật (sắp xếp theo ngày gần nhất, giới hạn 5 sự kiện)
    @Query("SELECT e FROM Event e WHERE e.status = 'approved' ORDER BY e.date ASC LIMIT 5")
    List<Event> findFeaturedPublicEvents();

    // Tìm kiếm sự kiện công khai theo tên hoặc địa điểm
    @Query("SELECT e FROM Event e WHERE e.status = 'approved' AND " +
            "(e.name LIKE %:keyword% OR e.location LIKE %:keyword%)")
    List<Event> searchPublicEvents(@Param("keyword") String keyword);

    // Phương thức mới để lấy 4 sự kiện có banner
    @Query("SELECT e FROM Event e WHERE e.status = 'approved' AND e.bannerUrl IS NOT NULL ORDER BY e.date ASC LIMIT 4")
    List<Event> findTop4EventsWithBanner();

    // Lọc sự kiện công khai theo danh mục, ngày, địa điểm
    @Query("SELECT e FROM Event e WHERE e.status = 'approved' " +
            "AND (:categoryId IS NULL OR e.categoryId = :categoryId) " +
            "AND (:location IS NULL OR e.location LIKE %:location%)")
    List<Event> findByCategoryIdAndLocation(@Param("categoryId") Integer categoryId,
                                            @Param("location") String location);

    @Query("SELECT e FROM Event e WHERE e.status = 'approved' " +
            "AND (:categoryId IS NULL OR e.categoryId = :categoryId) " +
            "AND e.date >= :dateFrom AND e.date <= :dateTo " +
            "AND (:location IS NULL OR e.location LIKE %:location%)")
    List<Event> findByCategoryIdAndDateRangeAndLocation(@Param("categoryId") Integer categoryId,
                                                        @Param("dateFrom") LocalDate dateFrom,
                                                        @Param("dateTo") LocalDate dateTo,
                                                        @Param("location") String location);
}