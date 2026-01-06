package com.popcorn.demo.domain.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.entity.enums.UserRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String email;
    private String phone;
    private String name;
    private LocalDate birthDate;
    private UserRole role;
    private Boolean isActive;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User 엔티티에서 UserResponse로 변환
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                //.phone(formatPhone(user.getPhone())) 
                .phone(user.getPhone())
                .name(user.getName())
                .role(user.getRole())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}