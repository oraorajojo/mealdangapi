package com.example.mealdangapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.mealdangapi.user.security.CustomUserDetailsService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Authorization 헤더 가져오기
        String authorizationHeader =
                request.getHeader("Authorization");

        // JWT가 없는 경우
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // "Bearer " 제거
        String token =
                authorizationHeader.substring(7);

        try {

            // JWT 유효성 검증
            if (jwtTokenProvider.validateToken(token)) {

                // JWT에서 이메일 추출
                String email =
                        jwtTokenProvider.getEmail(token);

                // 사용자 정보 조회
                UserDetails userDetails =
                        customUserDetailsService
                                .loadUserByUsername(email);

                // 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // 요청 정보 등록
                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // SecurityContext에 인증 정보 저장
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception e) {

            // JWT가 잘못된 경우 인증하지 않고 다음 필터로 진행
            SecurityContextHolder.clearContext();
        }

        // 다음 필터 실행
        filterChain.doFilter(request, response);
    }
}