package com.example.eventservice.controller;

import com.example.eventservice.dto.FavouriteEventDto;
import com.example.eventservice.dto.ResponseWrapper;
import com.example.eventservice.service.FavouriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class FavouriteController {

    @Autowired
    private FavouriteService favouriteService;

    @PostMapping("/{eventId}/favourite")
    public ResponseEntity<ResponseWrapper<String>> addFavourite(@PathVariable Integer eventId) {
        Integer userId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        ResponseWrapper<String> response = favouriteService.addFavourite(userId, eventId);

        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{eventId}/favourite")
    public ResponseEntity<ResponseWrapper<String>> removeFavourite(@PathVariable Integer eventId) {
        Integer userId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        ResponseWrapper<String> response = favouriteService.removeFavourite(userId, eventId);

        if ("success".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/favourites")
    public ResponseEntity<ResponseWrapper<List<FavouriteEventDto>>> getFavourites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer userId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        ResponseWrapper<List<FavouriteEventDto>> response = favouriteService.getFavourites(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{eventId}/favourite/status")
    public ResponseEntity<ResponseWrapper<Boolean>> getFavouriteStatus(@PathVariable Integer eventId) {
        Integer userId = Integer
                .parseInt((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        // Tạo method mới trong service để kiểm tra trạng thái favourite
        boolean isFavourite = favouriteService.isFavourite(userId, eventId);
        ResponseWrapper<Boolean> response = new ResponseWrapper<>("success", "Favourite status retrieved", isFavourite);

        return ResponseEntity.ok(response);
    }
}