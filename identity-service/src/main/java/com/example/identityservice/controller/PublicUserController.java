package com.example.identityservice.controller;

import com.example.identityservice.dto.UserPublicDto;
import com.example.identityservice.model.User;
import com.example.identityservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/public")
public class PublicUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserPublicDto> getUserPublicInfo(@PathVariable Integer userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        UserPublicDto publicDto = new UserPublicDto();
        publicDto.setUserId(user.getUserId());
        publicDto.setUsername(user.getUsername());
        publicDto.setEmail(user.getEmail());
        publicDto.setFullName(user.getFullName());
        publicDto.setPhoneNumber(user.getPhone());

        return ResponseEntity.ok(publicDto);
    }
}