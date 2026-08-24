package com.example.mealdangapi.user.dto;

import com.example.mealdangapi.user.entity.CookingLevel;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.entity.UserRole;

/**
 * 로그인류 API(일반/카카오 로그인/카카오 가입) 공통 응답.
 *
 * 이전엔 {message, accessToken}만 내려줘서, 프론트가 로그인 직후에 닉네임·요리숙련도를
 * 알 방법이 없어 화면에 "회원"/"보통" 같은 기본값만 떴다. 로그인 응답에 사용자 정보를
 * 같이 실어서 별도로 /me를 다시 부르지 않아도 되게 한다.
 */
public record LoginResponse(
        String message,
        String accessToken,
        Long userId,
        String email,
        String nickname,
        CookingLevel cookingLevel,
        UserRole role
) {
    public static LoginResponse of(String message, String accessToken, User user) {
        return new LoginResponse(
                message,
                accessToken,
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getCookingLevel(),
                user.getRole()
        );
    }
}
