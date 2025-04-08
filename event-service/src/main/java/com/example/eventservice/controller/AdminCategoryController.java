package com.example.eventservice.controller;

import com.example.eventservice.dto.CategoryRequestDto;
import com.example.eventservice.dto.CategoryResponseDto;
import com.example.eventservice.model.Category;
import com.example.eventservice.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminCategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequestDto requestDto) {
        Category category = categoryService.createCategory(requestDto);
        CategoryResponseDto response = new CategoryResponseDto(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                "Tạo danh mục thành công"
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}