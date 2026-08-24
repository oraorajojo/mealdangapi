package com.example.mealdangapi.user.controller;

import com.example.mealdangapi.user.dto.KakaoCodeLoginRequest;
import com.example.mealdangapi.user.dto.KakaoLoginResponse;
import com.example.mealdangapi.user.dto.KakaoSignupRequest;
import com.example.mealdangapi.user.dto.LoginRequest;
import com.example.mealdangapi.user.dto.LoginResponse;
import com.example.mealdangapi.user.dto.UserResponse;
import com.example.mealdangapi.user.dto.UserSignupRequest;
import com.example.mealdangapi.user.dto.WithdrawRequest;
import com.example.mealdangapi.user.service.KakaoService;
import com.example.mealdangapi.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users" )
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final KakaoService kakaoService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @RequestBody UserSignupRequest request
    ) {
        Long userId = userService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        Map.of(
                                "message", "회원가입이 완료되었습니다.",
                                "userId", userId
                        )
                );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(
                Map.of(
                        "message", "로그아웃이 완료되었습니다."
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            Authentication authentication
    ) {
        UserResponse response = userService.getMyInfo(
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> withdraw(
            Authentication authentication,
            @RequestBody WithdrawRequest request
    ) {
        userService.withdraw(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "회원탈퇴가 완료되었습니다."
                )
        );
    }

    /**
     * 카카오 이메일 수신 API.
     * 응답으로 받은 email을 프론트 회원가입 화면의 이메일 입력칸에 넣는다.
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<KakaoLoginResponse> kakaoCallback(
            @RequestParam("code") String code
    ) {
        KakaoLoginResponse response = kakaoService.getKakaoEmail(code);

        return ResponseEntity.ok(response);
    }

    /**
     * 카카오 로그인. 이미 가입된 계정이면 바로 로그인(200). 처음 보는 계정이면
     * 자동 가입하지 않고, 프론트가 닉네임·요리 숙련도 입력 화면으로 보낼 수 있도록
     * 404 + needsSignup 신호를 내려준다. 실제 가입은 POST /signup/kakao가 담당한다.
     */
    @PostMapping("/login/kakao")
    public ResponseEntity<?> loginWithKakao(
            @RequestBody KakaoCodeLoginRequest request
    ) {
        KakaoService.KakaoUserInfo kakaoUser = kakaoService.fetchKakaoUser(
                request.code(),
                request.redirectUri()
        );

        return userService.loginWithKakaoIfExists(kakaoUser.email(), kakaoUser.kakaoId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "needsSignup", true,
                                "email", kakaoUser.email(),
                                "suggestedNickname", kakaoUser.nickname() == null ? "" : kakaoUser.nickname()
                        )));
    }

    /**
     * 카카오 신규 가입 마무리(닉네임·요리 숙련도 직접 선택 후 제출).
     */
    @PostMapping("/signup/kakao")
    public ResponseEntity<LoginResponse> signupWithKakao(
            @RequestBody KakaoSignupRequest request
    ) {
        LoginResponse response = userService.signupWithKakao(
                request.email(),
                request.nickname(),
                request.cookingLevel()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
