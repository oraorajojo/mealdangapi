package com.example.mealdangapi.user.dto;

import com.example.mealdangapi.user.entity.CookingLevel;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.entity.UserRole;
import com.example.mealdangapi.user.entity.UserStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final CookingLevel cookingLevel;
    private final UserRole role;
    private final UserStatus status;
    private final LocalDateTime createdAt;

    public UserResponse(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.cookingLevel = user.getCookingLevel();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.createdAt = user.getCreatedAt();
    }
}