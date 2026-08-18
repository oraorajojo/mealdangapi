package com.example.mealdangapi.user.service;

import com.example.mealdangapi.security.JwtTokenProvider;
import com.example.mealdangapi.user.dto.LoginRequest;
import com.example.mealdangapi.user.dto.UserResponse;
import com.example.mealdangapi.user.dto.UserSignupRequest;
import com.example.mealdangapi.user.dto.WithdrawRequest;
import com.example.mealdangapi.user.entity.SocialProvider;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.entity.UserStatus;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // =========================================================
    // 비밀번호 암호화 설정
    // =========================================================

    private static final int SALT_LENGTH = 16;

    private static final int HASH_LENGTH = 256;

    private static final int ITERATIONS = 120_000;


    // =========================================================
    // 회원가입
    // =========================================================

    @Transactional
    public Long signup(UserSignupRequest request) {

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }

        // 닉네임 중복 확인
        if (userRepository.existsByNickname(request.getNickname())) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }

        // 비밀번호 확인
        if (!request.getPassword()
                .equals(request.getPasswordConfirm())) {

            throw new IllegalArgumentException(
                    "비밀번호가 일치하지 않습니다."
            );
        }

        // 비밀번호 암호화
        String passwordHash =
                hashPassword(request.getPassword());

        // 회원 생성
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .socialProvider(SocialProvider.NONE)
                .nickname(request.getNickname())
                .cookingLevel(request.getCookingLevel())
                .build();

        // 회원 저장
        User savedUser =
                userRepository.save(user);

        return savedUser.getUserId();
    }


    // =========================================================
    // 로그인
    // =========================================================

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {

        // 이메일로 회원 조회
        User user =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "이메일 또는 비밀번호가 올바르지 않습니다."
                                )
                        );

        // 탈퇴 회원 로그인 차단
        if (user.getStatus() == UserStatus.WITHDRAWN) {

            throw new IllegalArgumentException(
                    "탈퇴한 회원은 로그인할 수 없습니다."
            );
        }

        // 정지 회원 로그인 차단
        if (user.getStatus() == UserStatus.SUSPENDED) {

            throw new IllegalArgumentException(
                    "정지된 회원입니다."
            );
        }

        // 비밀번호 검증
        if (!verifyPassword(
                request.getPassword(),
                user.getPasswordHash()
        )) {

            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        // JWT 발급
        return jwtTokenProvider.generateToken(
                user.getEmail()
        );
    }


    // =========================================================
    // 회원탈퇴
    // =========================================================

    @Transactional
    public void withdraw(
            String email,
            WithdrawRequest request
    ) {

        // 현재 로그인한 회원 조회
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "사용자를 찾을 수 없습니다."
                                )
                        );

        // 이미 탈퇴한 회원인지 확인
        if (user.getStatus() == UserStatus.WITHDRAWN) {

            throw new IllegalArgumentException(
                    "이미 탈퇴한 회원입니다."
            );
        }

        // 탈퇴 사유 확인
        if (request == null ||
                request.getReasonCode() == null) {

            throw new IllegalArgumentException(
                    "탈퇴 사유를 선택해주세요."
            );
        }

        // 기타 선택 시 세부 내용 확인
        if (request.getReasonCode().name().equals("ETC")) {

            if (request.getEtcReason() == null ||
                    request.getEtcReason().trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "기타 탈퇴 사유를 입력해주세요."
                );
            }
        }

        // 회원탈퇴 처리
        user.withdraw(
                request.getReasonCode(),
                request.getEtcReason()
        );
    }


    // =========================================================
    // 내 정보 조회
    // =========================================================

    @Transactional(readOnly = true)
    public UserResponse getMyInfo(
            String email
    ) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "사용자를 찾을 수 없습니다."
                                )
                        );

        return new UserResponse(user);
    }


    // =========================================================
    // 비밀번호 암호화
    // =========================================================

    private String hashPassword(
            String password
    ) {

        try {

            // 랜덤 Salt 생성
            SecureRandom secureRandom =
                    new SecureRandom();

            byte[] salt =
                    new byte[SALT_LENGTH];

            secureRandom.nextBytes(salt);

            // PBKDF2 설정
            PBEKeySpec spec =
                    new PBEKeySpec(
                            password.toCharArray(),
                            salt,
                            ITERATIONS,
                            HASH_LENGTH
                    );

            // PBKDF2-HMAC-SHA256
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            // Hash 생성
            byte[] hash =
                    factory.generateSecret(spec)
                            .getEncoded();

            // DB 저장 형식
            //
            // 반복횟수:Salt:Hash
            //
            return ITERATIONS
                    + ":"
                    + Base64.getEncoder()
                    .encodeToString(salt)
                    + ":"
                    + Base64.getEncoder()
                    .encodeToString(hash);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "비밀번호 암호화에 실패했습니다.",
                    e
            );
        }
    }


    // =========================================================
    // 비밀번호 검증
    // =========================================================

    private boolean verifyPassword(
            String password,
            String storedPassword
    ) {

        try {

            // 저장된 비밀번호 분리
            //
            // 120000:Salt:Hash
            //
            String[] parts =
                    storedPassword.split(":");

            // 반복 횟수
            int iterations =
                    Integer.parseInt(parts[0]);

            // Salt 복원
            byte[] salt =
                    Base64.getDecoder()
                            .decode(parts[1]);

            // 기존 Hash 복원
            byte[] storedHash =
                    Base64.getDecoder()
                            .decode(parts[2]);

            // 입력받은 비밀번호를
            // 동일한 방식으로 Hash
            PBEKeySpec spec =
                    new PBEKeySpec(
                            password.toCharArray(),
                            salt,
                            iterations,
                            HASH_LENGTH
                    );

            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256"
                    );

            byte[] hash =
                    factory.generateSecret(spec)
                            .getEncoded();

            // Hash 비교
            return MessageDigest.isEqual(
                    hash,
                    storedHash
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "비밀번호 검증에 실패했습니다.",
                    e
            );
        }
    }
}