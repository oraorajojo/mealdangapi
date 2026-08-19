package com.example.mealdangapi.security;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/error",
                                "/api/users/signup",
                                "/api/users/login",
                                "/api/users/kakao/callback"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/recommend"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/recipes/**",
                                "/api/board/**",
                                "/api/ingredients/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build( );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
