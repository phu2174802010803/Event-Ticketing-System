package com.example.identityservice.dto;

import lombok.Data;

@Data
public class OrganizerProfileDto {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String organizationName;
    private String contactEmail;
    private String description;
}