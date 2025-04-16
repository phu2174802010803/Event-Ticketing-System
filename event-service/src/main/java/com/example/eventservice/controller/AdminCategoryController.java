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

import java.util.List;
import java.util.stream.Collectors;

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

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable Integer categoryId) {
        CategoryResponseDto response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Integer categoryId,
            @Valid @RequestBody CategoryRequestDto requestDto) {
        Category category = categoryService.updateCategory(categoryId, requestDto);
        CategoryResponseDto response = new CategoryResponseDto(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                "Cập nhật danh mục thành công"
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Integer categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok("Xóa danh mục thành công");
    }
}