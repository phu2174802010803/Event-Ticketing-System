package com.example.identityservice.controller;

import com.example.identityservice.dto.UserResponseDto;
import com.example.identityservice.model.User;
import com.example.identityservice.service.UserService;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDto> getProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        return ResponseEntity.ok(new UserResponseDto(user.getUserId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getPhone(), user.getAddress()));
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody UserResponseDto updateDto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        // Cập nhật chỉ các trường được gửi trong request
        if (updateDto.getEmail() != null) {
            user.setEmail(updateDto.getEmail());
        }
        if (updateDto.getFullName() != null) {
            user.setFullName(updateDto.getFullName());
        }
        if (updateDto.getPhone() != null) {
            user.setPhone(updateDto.getPhone());
        }
        if (updateDto.getAddress() != null) {
            user.setAddress(updateDto.getAddress());
        }
        user.setUpdatedAt(LocalDateTime.now());
        
        userService.save(user);
        return ResponseEntity.ok("Cập nhật thông tin thành công");
    }
}