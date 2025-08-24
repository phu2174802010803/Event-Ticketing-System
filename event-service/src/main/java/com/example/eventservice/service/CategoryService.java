package com.example.eventservice.service;

import com.example.eventservice.dto.CategoryRequestDto;
import com.example.eventservice.dto.CategoryResponseDto;
import com.example.eventservice.exception.CategoryAlreadyExistsException;
import com.example.eventservice.model.Category;
import com.example.eventservice.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Transactional
    public Category createCategory(CategoryRequestDto requestDto) {
        if (categoryRepository.existsByName(requestDto.getName())) {
            throw new CategoryAlreadyExistsException("Danh mục với tên '" + requestDto.getName() + "' đã tồn tại.");
        }
        Category category = new Category();
        category.setName(requestDto.getName());
        category.setDescription(requestDto.getDescription());
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    //Lấy thông tin danh mục theo ID
    public CategoryResponseDto getCategoryById(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
        return new CategoryResponseDto(category.getCategoryId(), category.getName(), category.getDescription(), null);
    }

    @Transactional
    public Category updateCategory(Integer categoryId, CategoryRequestDto requestDto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
        if (!category.getName().equals(requestDto.getName()) && categoryRepository.existsByName(requestDto.getName())) {
            throw new CategoryAlreadyExistsException("Danh mục với tên '" + requestDto.getName() + "' đã tồn tại.");
        }
        category.setName(requestDto.getName());
        category.setDescription(requestDto.getDescription());
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Integer categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
        categoryRepository.delete(category);
    }

    //Kiểm tra xem danh mục có tồn tại hay không
    public boolean categoryExists(Integer categoryId) {
        return categoryRepository.existsById(categoryId);
    }
}