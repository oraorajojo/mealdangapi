package com.example.mealdangapi.user.service;

import com.example.mealdangapi.user.dto.LoginRequest;
import com.example.mealdangapi.user.dto.UserSignupRequest;
import com.example.mealdangapi.user.entity.SocialProvider;
import com.example.mealdangapi.user.entity.User;
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

    // 비밀번호 암호화 설정
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
    public Long login(LoginRequest request) {

        // 이메일로 회원 조회
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "이메일 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        // 비밀번호 검증
        if (!verifyPassword(
                request.getPassword(),
                user.getPasswordHash()
        )) {

            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        // 로그인 성공
        return user.getUserId();
    }


    // =========================================================
    // 비밀번호 암호화
    // =========================================================

    private String hashPassword(String password) {

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