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

        boolean hasChanges = false;
        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(user.getEmail())) {
            if (userService.findByEmail(updateDto.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
            user.setEmail(updateDto.getEmail());
            hasChanges = true;
        }
        if (updateDto.getFullName() != null && !updateDto.getFullName().equals(user.getFullName())) {
            user.setFullName(updateDto.getFullName());
            hasChanges = true;
        }
        if (updateDto.getPhone() != null && !updateDto.getPhone().equals(user.getPhone())) {
            user.setPhone(updateDto.getPhone());
            hasChanges = true;
        }
        if (updateDto.getAddress() != null && !updateDto.getAddress().equals(user.getAddress())) {
            user.setAddress(updateDto.getAddress());
            hasChanges = true;
        }

        if (!hasChanges) {
            return ResponseEntity.ok("Không có thay đổi để cập nhật");
        }

        user.setUpdatedAt(LocalDateTime.now());
        userService.save(user);
        return ResponseEntity.ok("Cập nhật thông tin thành công");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Integer userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        return ResponseEntity.ok(new UserResponseDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getRole().toString(),
                user.getCreatedAt(),
                user.isActive()));
    }
}