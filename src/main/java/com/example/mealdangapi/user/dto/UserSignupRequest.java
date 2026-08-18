package com.example.mealdangapi.user.dto;

import com.example.mealdangapi.user.entity.CookingLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSignupRequest {

    private String email;

    private String password;

    private String passwordConfirm;

    private String nickname;

    private CookingLevel cookingLevel;
}