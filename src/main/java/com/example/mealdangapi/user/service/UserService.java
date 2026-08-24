package com.example.mealdangapi.user.service;

import com.example.mealdangapi.admin.service.AdminUserService;
import com.example.mealdangapi.security.JwtTokenProvider;
import com.example.mealdangapi.user.dto.LoginRequest;
import com.example.mealdangapi.user.dto.UserResponse;
import com.example.mealdangapi.user.dto.UserSignupRequest;
import com.example.mealdangapi.user.dto.WithdrawRequest;
import com.example.mealdangapi.user.entity.CookingLevel;
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
    private final AdminUserService adminUserService;

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 256;
    private static final int ITERATIONS = 120_000;

    @Transactional
    public Long signup(UserSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String passwordHash = hashPassword(request.getPassword());

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .socialProvider(SocialProvider.NONE)
                .nickname(request.getNickname())
                .cookingLevel(request.getCookingLevel())
                .build();

        User savedUser = userRepository.save(user);
        return savedUser.getUserId();
    }

    @Transactional(readOnly = true)
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException(
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new IllegalArgumentException("탈퇴한 회원은 로그인할 수 없습니다.");
        }

        if (adminUserService.isEffectivelySuspended(user)) {
            throw new IllegalArgumentException("정지된 회원입니다.");
        }

        if (!verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        return jwtTokenProvider.generateToken(user.getEmail());
    }

    /**
     * 카카오 원클릭 로그인/가입. 비밀번호가 없으므로 일반 로그인과 달리
     * "계정이 없으면 그 자리에서 만든다"(로그인=가입)로 처리한다.
     *
     * social_id(카카오 고유ID) 우선 조회 → 없으면 email로 조회(과거 일반 가입 계정과
     * 같은 이메일이면 그 계정으로 로그인시킴, 이메일 UNIQUE라 신규 생성 시 충돌 방지도 겸함)
     * → 그래도 없으면 신규 생성.
     */
    @Transactional
    public String loginOrSignupWithKakao(String email, String kakaoId, String kakaoNickname) {
        User user = userRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, kakaoId)
                .or(() -> userRepository.findByEmail(email))
                .orElse(null);

        if (user == null) {
            user = User.builder()
                    .email(email)
                    .passwordHash(null)
                    .socialProvider(SocialProvider.KAKAO)
                    .socialId(kakaoId)
                    .nickname(generateUniqueNickname(kakaoNickname, email))
                    .cookingLevel(CookingLevel.BEGINNER)
                    .build();
            userRepository.save(user);
        } else {
            if (user.getStatus() == UserStatus.WITHDRAWN) {
                throw new IllegalArgumentException("탈퇴한 회원은 로그인할 수 없습니다.");
            }
            if (adminUserService.isEffectivelySuspended(user)) {
                throw new IllegalArgumentException("정지된 회원입니다.");
            }
        }

        return jwtTokenProvider.generateToken(user.getEmail());
    }

    /**
     * 요리 숙련도까지 직접 고른 카카오 신규 가입 마무리용.
     * 이미 가입된 이메일이면(예: 새로고침으로 중복 제출) 새로 만들지 않고 그냥 로그인시킨다.
     */
    @Transactional
    public String signupWithKakao(String email, String nickname, CookingLevel cookingLevel) {
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            return jwtTokenProvider.generateToken(existing.getEmail());
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(null)
                .socialProvider(SocialProvider.KAKAO)
                .nickname(nickname)
                .cookingLevel(cookingLevel)
                .build();
        userRepository.save(user);

        return jwtTokenProvider.generateToken(user.getEmail());
    }

    private String generateUniqueNickname(String preferred, String email) {
        String base = (preferred != null && !preferred.isBlank())
                ? preferred
                : email.substring(0, email.indexOf('@'));

        if (!userRepository.existsByNickname(base)) {
            return base;
        }
        for (int i = 0; i < 20; i++) {
            String candidate = base + "_" + (1000 + (int) (Math.random() * 9000));
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        return base + "_" + System.currentTimeMillis();
    }

    @Transactional
    public void withdraw(String email, WithdrawRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다."
                ));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }

        if (request == null || request.getReasonCode() == null) {
            throw new IllegalArgumentException("탈퇴 사유를 선택해주세요.");
        }

        if (request.getReasonCode().name().equals("ETC")) {
            if (request.getEtcReason() == null
                    || request.getEtcReason().trim().isEmpty()) {
                throw new IllegalArgumentException("기타 탈퇴 사유를 입력해주세요.");
            }
        }

        user.withdraw(request.getReasonCode(), request.getEtcReason());
    }

    @Transactional(readOnly = true)
    public UserResponse getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "사용자를 찾을 수 없습니다."
                ));

        return new UserResponse(user);
    }

    private String hashPassword(String password) {
        try {
            SecureRandom secureRandom = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    HASH_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
            );

            byte[] hash = factory.generateSecret(spec).getEncoded();

            return ITERATIONS
                    + ":"
                    + Base64.getEncoder().encodeToString(salt)
                    + ":"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("비밀번호 암호화에 실패했습니다.", e);
        }
    }

    private boolean verifyPassword(String password, String storedPassword) {
        try {
            String[] parts = storedPassword.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] storedHash = Base64.getDecoder().decode(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    iterations,
                    HASH_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
            );

            byte[] hash = factory.generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(hash, storedHash);
        } catch (Exception e) {
            throw new IllegalStateException("비밀번호 검증에 실패했습니다.", e);
        }
    }
}
