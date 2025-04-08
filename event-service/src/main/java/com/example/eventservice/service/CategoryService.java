package com.example.eventservice.service;

import com.example.eventservice.dto.CategoryRequestDto;
import com.example.eventservice.exception.CategoryAlreadyExistsException;
import com.example.eventservice.model.Category;
import com.example.eventservice.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}