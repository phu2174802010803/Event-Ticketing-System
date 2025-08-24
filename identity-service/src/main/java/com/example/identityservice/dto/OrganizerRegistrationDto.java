package com.example.identityservice.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizerRegistrationDto extends UserRegistrationDto {
    private String organizationName;
    private String contactEmail;
    private String description;
}