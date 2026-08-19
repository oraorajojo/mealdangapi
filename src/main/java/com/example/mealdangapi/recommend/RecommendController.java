package com.example.mealdangapi.recommend;

import com.example.mealdangapi.recommend.dto.RecommendRequest;
import com.example.mealdangapi.recommend.dto.RecommendResponse;
import com.example.mealdangapi.recommend.dto.SelectRequest;
import com.example.mealdangapi.recommend.dto.SelectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    // 비회원도 호출 가능. 단 토큰을 함께 보내면 로그인한 사용자로 recommend_logs에 기록된다
    // (그래야 이후 select에서 본인 소유 로그인지 검증 가능).
    @PostMapping
    public RecommendResponse recommend(Authentication authentication, @RequestBody RecommendRequest request) {
        return recommendService.recommend(authentication, request);
    }

    // 로그인 필수 (SecurityConfig에서 이 경로만 인증을 요구해야 함, "/api/recommend" 자체는 비회원도 허용)
    @PostMapping("/{recommendLogId}/select")
    public ResponseEntity<SelectResponse> select(
            @PathVariable Long recommendLogId,
            Authentication authentication,
            @RequestBody SelectRequest request
    ) {
        SelectResponse response = recommendService.select(recommendLogId, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
