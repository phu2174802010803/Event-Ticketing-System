package com.example.eventservice.dto;

import lombok.Data;

@Data
public class OrganizerPublicDto {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;

    public OrganizerPublicDto() {
    }

    public OrganizerPublicDto(Integer userId, String username, String email, String fullName, String phoneNumber) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }
}