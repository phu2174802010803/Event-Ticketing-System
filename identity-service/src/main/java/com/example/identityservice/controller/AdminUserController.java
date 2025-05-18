package com.example.identityservice.controller;

import com.example.identityservice.dto.ResponseWrapper;
import com.example.identityservice.dto.UserCreateDto;
import com.example.identityservice.dto.UserManagementResponseDto;
import com.example.identityservice.dto.UserUpdateDto;
import com.example.identityservice.service.UserService;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ResponseWrapper<Page<UserManagementResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserManagementResponseDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Danh sách người dùng", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper<UserManagementResponseDto>> getUserById(@PathVariable Integer id) {
        UserManagementResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Chi tiết người dùng", user));
    }

    @PostMapping
    public ResponseEntity<ResponseWrapper<UserManagementResponseDto>> createUser(@Valid @RequestBody UserCreateDto dto) {
        UserManagementResponseDto user = userService.createUser(dto);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Tạo người dùng thành công", user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseWrapper<UserManagementResponseDto>> updateUser(
            @PathVariable Integer id, @Valid @RequestBody UserUpdateDto dto) {
        UserManagementResponseDto user = userService.updateUser(id, dto);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Cập nhật người dùng thành công", user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseWrapper<String>> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Xóa người dùng thành công", null));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ResponseWrapper<String>> activateUser(@PathVariable Integer id) {
        userService.activateUser(id);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Kích hoạt tài khoản thành công", null));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ResponseWrapper<String>> deactivateUser(@PathVariable Integer id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Vô hiệu hóa tài khoản thành công", null));
    }
}

