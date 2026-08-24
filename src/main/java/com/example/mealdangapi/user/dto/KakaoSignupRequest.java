package com.example.mealdangapi.user.dto;

import com.example.mealdangapi.user.entity.CookingLevel;

/** 카카오 신규 가입 마무리 요청 (POST /api/users/signup/kakao) */
public record KakaoSignupRequest(
        String email,
        String nickname,
        CookingLevel cookingLevel
) {
}
