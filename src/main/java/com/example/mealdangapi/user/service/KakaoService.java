package com.example.mealdangapi.user.service;

import com.example.mealdangapi.user.dto.KakaoLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoService {

    private final RestClient restClient;

    @Value("${kakao.client-id}" )
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    /**
     * 카카오에서 이메일만 받아 회원가입 화면에 전달한다.
     * users 테이블 저장과 JWT 발급은 기존 일반 회원가입 API에서 처리한다.
     */
    @Transactional(readOnly = true)
    public KakaoLoginResponse getKakaoEmail(String code) {
        String kakaoAccessToken = getAccessToken(code);
        Map<String, Object> userInfo = getUserInfo(kakaoAccessToken);
        String email = getRequiredEmail(userInfo);

        return new KakaoLoginResponse(email);
    }

    private String getAccessToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);

        Map<?, ?> response = restClient
                .post()
                .uri("https://kauth.kakao.com/oauth/token" )
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);

        if (response == null
                || !(response.get("access_token") instanceof String accessToken)
                || accessToken.isBlank()) {

            throw new IllegalArgumentException(
                    "카카오 Access Token 발급에 실패했습니다. Redirect URI와 REST API 키를 확인해주세요."
            );
        }

        return accessToken;
    }

    private Map<String, Object> getUserInfo(String accessToken) {
        Map<?, ?> response = restClient
                .get()
                .uri("https://kapi.kakao.com/v2/user/me" )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalArgumentException(
                    "카카오 사용자 정보를 가져오지 못했습니다."
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) response;

        return userInfo;
    }

    private String getRequiredEmail(Map<String, Object> userInfo) {
        Object kakaoAccountValue = userInfo.get("kakao_account");

        if (!(kakaoAccountValue instanceof Map<?, ?> kakaoAccount)) {
            throw new IllegalArgumentException(
                    "카카오 계정 정보를 가져올 수 없습니다."
            );
        }

        Object emailValue = kakaoAccount.get("email");

        if (!(emailValue instanceof String email) || email.isBlank()) {
            throw new IllegalArgumentException(
                    "카카오 이메일 정보를 가져올 수 없습니다. 카카오 이메일 동의항목을 확인해주세요."
            );
        }

        return email;
    }
}
