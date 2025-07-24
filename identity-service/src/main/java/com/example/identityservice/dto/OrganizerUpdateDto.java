package com.example.identityservice.dto;

import lombok.Data;

@Data
public class OrganizerUpdateDto {
    private String fullName;
    private String phone;
    private String address;
    private String organizationName;
    private String contactEmail;
    private String description;
}