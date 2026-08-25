package com.example.mealdangapi.user.service;

import com.example.mealdangapi.admin.service.AdminUserService;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.security.JwtTokenProvider;
import com.example.mealdangapi.user.dto.LoginRequest;
import com.example.mealdangapi.user.dto.LoginResponse;
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
import java.util.Optional;

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
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException(
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN, "탈퇴한 회원은 로그인할 수 없습니다.");
        }

        if (adminUserService.isEffectivelySuspended(user)) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED, "정지된 회원입니다.");
        }

        if (!verifyPassword(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        return LoginResponse.of("로그인이 완료되었습니다.", accessToken, user);
    }

    /**
     * 카카오 로그인. 이미 가입된 계정(social_id 또는 같은 이메일)이 있을 때만 로그인시킨다.
     * 없으면 여기서 자동 가입하지 않고 빈 Optional을 돌려준다 — 컨트롤러가 이를 보고
     * "가입 필요" 응답을 내려서, 프론트가 닉네임·요리 숙련도를 입력받는 화면으로 보낸다.
     * (신규 가입 자체는 signupWithKakao가 담당)
     */
    @Transactional(readOnly = true)
    public Optional<LoginResponse> loginWithKakaoIfExists(String email, String kakaoId) {
        return userRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, kakaoId)
                .or(() -> userRepository.findByEmail(email))
                .map(user -> {
                    if (user.getStatus() == UserStatus.WITHDRAWN) {
                        throw new BusinessException(ErrorCode.ACCOUNT_WITHDRAWN, "탈퇴한 회원은 로그인할 수 없습니다.");
                    }
                    if (adminUserService.isEffectivelySuspended(user)) {
                        throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED, "정지된 회원입니다.");
                    }
                    String accessToken = jwtTokenProvider.generateToken(user.getEmail());
                    return LoginResponse.of("카카오 로그인이 완료되었습니다.", accessToken, user);
                });
    }

    /**
     * 요리 숙련도까지 직접 고른 카카오 신규 가입 마무리용.
     * 이미 가입된 이메일이면(예: 새로고침으로 중복 제출) 새로 만들지 않고 그냥 로그인시킨다.
     */
    @Transactional
    public LoginResponse signupWithKakao(String email, String nickname, CookingLevel cookingLevel) {
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            String accessToken = jwtTokenProvider.generateToken(existing.getEmail());
            return LoginResponse.of("카카오 회원가입이 완료되었습니다.", accessToken, existing);
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

        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        return LoginResponse.of("카카오 회원가입이 완료되었습니다.", accessToken, user);
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
