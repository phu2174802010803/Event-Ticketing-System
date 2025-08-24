package com.example.eventservice.service;

import com.example.eventservice.dto.FavouriteEventDto;
import com.example.eventservice.dto.ResponseWrapper;
import com.example.eventservice.model.Event;
import com.example.eventservice.model.Favourite;
import com.example.eventservice.repository.EventRepository;
import com.example.eventservice.repository.FavouriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavouriteService {

    @Autowired
    private FavouriteRepository favouriteRepository;

    @Autowired
    private EventRepository eventRepository;

    @Transactional
    public ResponseWrapper<String> addFavourite(Integer userId, Integer eventId) {
        // Kiểm tra xem sự kiện đã có trong danh sách yêu thích chưa
        if (favouriteRepository.findByUserIdAndEventEventId(userId, eventId).isPresent()) {
            return new ResponseWrapper<>("error", "Event already in favourites", null);
        }

        // Kiểm tra sự kiện có tồn tại không
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // Tạo bản ghi mới
        Favourite favourite = new Favourite();
        favourite.setUserId(userId);
        favourite.setEvent(event);
        favouriteRepository.save(favourite);

        return new ResponseWrapper<>("success", "Event added to favourites", null);
    }

    public ResponseWrapper<List<FavouriteEventDto>> getFavourites(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Favourite> favourites = favouriteRepository.findByUserId(userId, pageable);

        List<FavouriteEventDto> dtos = favourites.stream()
                .map(f -> mapToFavouriteEventDto(f.getEvent()))
                .collect(Collectors.toList());

        return new ResponseWrapper<>("success", "Favourites retrieved successfully", dtos);
    }

    @Transactional
    public ResponseWrapper<String> removeFavourite(Integer userId, Integer eventId) {
        Optional<Favourite> favourite = favouriteRepository.findByUserIdAndEventEventId(userId, eventId);
        if (favourite.isPresent()) {
            favouriteRepository.deleteByUserIdAndEventEventId(userId, eventId);
            return new ResponseWrapper<>("success", "Event removed from favourites", null);
        } else {
            return new ResponseWrapper<>("error", "Event not found in favourites", null);
        }
    }

    public boolean isFavourite(Integer userId, Integer eventId) {
        return favouriteRepository.findByUserIdAndEventEventId(userId, eventId).isPresent();
    }

    private FavouriteEventDto mapToFavouriteEventDto(Event event) {
        FavouriteEventDto dto = new FavouriteEventDto();
        dto.setEventId(event.getEventId());
        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setDate(event.getDate().toString());
        dto.setTime(event.getTime().toString());
        dto.setLocation(event.getLocation());
        dto.setStatus(event.getStatus());
        dto.setImageUrl(event.getImageUrl());
        dto.setBannerUrl(event.getBannerUrl());
        return dto;
    }
}