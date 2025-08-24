package com.example.eventservice.controller;

import com.example.eventservice.dto.CategoryResponseDto;
import com.example.eventservice.model.Category;
import com.example.eventservice.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerCategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        List<CategoryResponseDto> response = categories.stream()
                .map(category -> new CategoryResponseDto(
                        category.getCategoryId(),
                        category.getName(),
                        category.getDescription(),
                        null
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}