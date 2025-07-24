package com.example.identityservice.controller;

import com.example.identityservice.dto.OrganizerProfileDto;
import com.example.identityservice.dto.OrganizerUpdateDto;
import com.example.identityservice.dto.ResponseWrapper;
import com.example.identityservice.service.UserService;
import com.example.identityservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/profile")
    public ResponseEntity<ResponseWrapper<OrganizerProfileDto>> getProfile(HttpServletRequest request) {
        Integer userId = extractUserIdFromRequest(request);
        OrganizerProfileDto profile = userService.getOrganizerProfile(userId);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Organizer profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ResponseWrapper<String>> updateProfile(@RequestBody OrganizerUpdateDto updateDto,
                                                                 HttpServletRequest request) {
        Integer userId = extractUserIdFromRequest(request);
        userService.updateOrganizerProfile(userId, updateDto);
        return ResponseEntity.ok(new ResponseWrapper<>("success", "Organizer profile updated successfully", null));
    }

    private Integer extractUserIdFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);
            return jwtUtil.extractUserId(jwt);
        }
        throw new IllegalStateException("No valid JWT token found");
    }
}