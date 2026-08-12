package com.example.mealdangapi.user.controller;

import com.example.mealdangapi.user.dto.LoginRequest;
import com.example.mealdangapi.user.dto.UserSignupRequest;
import com.example.mealdangapi.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(
            @RequestBody UserSignupRequest request
    ) {

        Long userId = userService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "회원가입이 완료되었습니다.",
                        "userId", userId
                ));
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest request
    ) {

        Long userId = userService.login(request);

        return ResponseEntity.ok(
                Map.of(
                        "message", "로그인이 완료되었습니다.",
                        "userId", userId
                )
        );
    }
}