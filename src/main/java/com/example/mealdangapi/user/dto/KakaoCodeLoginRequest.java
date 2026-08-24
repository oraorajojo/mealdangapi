package com.example.mealdangapi.user.dto;

/** 카카오 원클릭 로그인 요청 (POST /api/users/login/kakao) */
public record KakaoCodeLoginRequest(
        String code,
        String redirectUri
) {
}
