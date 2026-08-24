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
import org.springframework.web.client.RestClientException;

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
        String kakaoAccessToken = getAccessToken(code, redirectUri);
        Map<String, Object> userInfo = getUserInfo(kakaoAccessToken);
        String email = getRequiredEmail(userInfo);

        return new KakaoLoginResponse(email);
    }

    /**
     * 비밀번호 없는 카카오 원클릭 로그인/가입용. email·카카오 고유ID(social_id)·
     * 카카오 프로필 닉네임(있으면)을 뽑아온다. users 저장은 UserService가 담당한다.
     *
     * redirectUri를 매번 인자로 받는 이유: 카카오는 인가코드 발급 때 쓴 redirect_uri와
     * 토큰 교환 때 쓴 redirect_uri가 정확히 같아야 한다. 프론트가 실제로 인가받을 때
     * 쓴 값(VITE_KAKAO_REDIRECT_URI)을 그대로 넘겨받아 써야, 서버 설정값(kakao.redirect-uri)과
     * 프론트 값이 어긋나 있어도(로컬 개발 환경마다 다를 수 있음) 안전하게 동작한다.
     */
    @Transactional(readOnly = true)
    public KakaoUserInfo fetchKakaoUser(String code, String redirectUriFromClient) {
        String uri = (redirectUriFromClient == null || redirectUriFromClient.isBlank())
                ? redirectUri
                : redirectUriFromClient;

        String kakaoAccessToken = getAccessToken(code, uri);
        Map<String, Object> userInfo = getUserInfo(kakaoAccessToken);
        String email = getRequiredEmail(userInfo);
        String kakaoId = String.valueOf(userInfo.get("id"));
        String nickname = extractNickname(userInfo);

        return new KakaoUserInfo(email, kakaoId, nickname);
    }

    public record KakaoUserInfo(String email, String kakaoId, String nickname) {
    }

    private String extractNickname(Map<String, Object> userInfo) {
        if (userInfo.get("kakao_account") instanceof Map<?, ?> kakaoAccount
                && kakaoAccount.get("profile") instanceof Map<?, ?> profile
                && profile.get("nickname") instanceof String nickname
                && !nickname.isBlank()) {
            return nickname;
        }

        if (userInfo.get("properties") instanceof Map<?, ?> properties
                && properties.get("nickname") instanceof String nickname
                && !nickname.isBlank()) {
            return nickname;
        }

        return null;
    }

    private String getAccessToken(String code, String redirectUriForExchange) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUriForExchange);
        formData.add("code", code);

        Map<?, ?> response;
        try {
            response = restClient
                    .post()
                    .uri("https://kauth.kakao.com/oauth/token" )
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            // 카카오가 code/redirect_uri 불일치 등으로 4xx를 주면 RestClient가 예외를 던진다.
            // 그대로 흘리면 500(원인 불명)이 되므로, 400으로 잡아서 원인을 알 수 있게 한다.
            throw new IllegalArgumentException(
                    "카카오 인증에 실패했습니다. 인가코드가 만료됐거나 Redirect URI가 일치하지 않습니다.", e
            );
        }

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
        Map<?, ?> response;
        try {
            response = restClient
                    .get()
                    .uri("https://kapi.kakao.com/v2/user/me" )
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new IllegalArgumentException("카카오 사용자 정보를 가져오지 못했습니다.", e);
        }

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
