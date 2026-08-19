package com.example.mealdangapi.admin.service;

import com.example.mealdangapi.admin.controller.AdminUserController;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.entity.UserRole;
import com.example.mealdangapi.user.entity.UserStatus;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String SUSPENSION_UNTIL_PREFIX = "SUSPENSION_UNTIL=";
    private static final DateTimeFormatter MEMO_DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public SuspensionResponse suspendUser(
            String adminEmail,
            Long targetUserId,
            AdminUserController.SuspensionRequest request
    ) {
        User admin = requireActiveAdmin(adminEmail);
        User targetUser = findTargetUser(targetUserId);
        validateSuspendRequest(admin, targetUser, request);

        if (isEffectivelySuspended(targetUser)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "이미 정지 처리 중인 회원입니다."
            );
        }

        LocalDateTime suspendedAt = LocalDateTime.now();
        LocalDateTime suspendedUntil = suspendedAt.plusDays(request.durationDays());

        jdbcTemplate.update(
                """
                UPDATE users
                SET status = 'SUSPENDED'
                WHERE user_id = :targetUserId
                """,
                new MapSqlParameterSource(
                        "targetUserId",
                        targetUser.getUserId()
                )
        );

        insertAdminAction(
                admin.getUserId(),
                targetUser.getUserId(),
                "SUSPEND_USER",
                buildSuspensionMemo(
                        request.durationDays(),
                        suspendedUntil,
                        request.memo()
                )
        );

        return new SuspensionResponse(
                targetUser.getUserId(),
                targetUser.getEmail(),
                targetUser.getNickname(),
                true,
                request.durationDays(),
                suspendedAt,
                suspendedUntil,
                "회원 정지 처리가 완료되었습니다."
        );
    }

    @Transactional
    public SuspensionResponse activateUser(
            String adminEmail,
            Long targetUserId
    ) {
        User admin = requireActiveAdmin(adminEmail);
        User targetUser = findTargetUser(targetUserId);

        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "관리자 계정은 정지 해제 대상으로 처리할 수 없습니다."
            );
        }

        jdbcTemplate.update(
                """
                UPDATE users
                SET status = 'ACTIVE'
                WHERE user_id = :targetUserId
                """,
                new MapSqlParameterSource(
                        "targetUserId",
                        targetUser.getUserId()
                )
        );

        insertAdminAction(
                admin.getUserId(),
                targetUser.getUserId(),
                "ACTIVATE_USER",
                "관리자 수동 정지 해제"
        );

        return new SuspensionResponse(
                targetUser.getUserId(),
                targetUser.getEmail(),
                targetUser.getNickname(),
                false,
                null,
                null,
                null,
                "회원 정지가 해제되었습니다."
        );
    }

    @Transactional(readOnly = true)
    public SuspensionStatusResponse getSuspensionStatus(
            String adminEmail,
            Long targetUserId
    ) {
        requireActiveAdmin(adminEmail);
        User targetUser = findTargetUser(targetUserId);
        SuspensionState state = resolveSuspensionState(targetUser);

        return new SuspensionStatusResponse(
                targetUser.getUserId(),
                targetUser.getEmail(),
                targetUser.getNickname(),
                state.suspended(),
                state.suspendedUntil(),
                state.remainingDays(),
                state.message()
        );
    }

    /**
     * 로그인 API와 JWT 필터가 공통으로 사용한다.
     * WITHDRAWN은 별도 회원 탈퇴 정책이므로 여기서는 정지 여부만 판단한다.
     */
    @Transactional(readOnly = true)
    public boolean isEffectivelySuspended(User user) {
        return resolveSuspensionState(user).suspended();
    }

    @Transactional(readOnly = true)
    public boolean isAccessAllowed(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .map(user -> user.getStatus() != UserStatus.WITHDRAWN
                        && !isEffectivelySuspended(user))
                .orElse(false);
    }

    private User requireActiveAdmin(String adminEmail) {
        if (!StringUtils.hasText(adminEmail)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (admin.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "활성 상태의 관리자만 처리할 수 있습니다."
            );
        }

        return admin;
    }

    private User findTargetUser(Long targetUserId) {
        if (targetUserId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "대상 회원 ID는 필수입니다."
            );
        }

        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "대상 회원을 찾을 수 없습니다."
                ));
    }

    private void validateSuspendRequest(
            User admin,
            User targetUser,
            AdminUserController.SuspensionRequest request
    ) {
        if (admin.getUserId().equals(targetUser.getUserId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "본인 계정은 정지 처리할 수 없습니다."
            );
        }

        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "관리자 계정은 정지 처리할 수 없습니다."
            );
        }

        if (targetUser.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "탈퇴한 회원은 정지 처리할 수 없습니다."
            );
        }

        if (request == null || request.durationDays() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "정지 기간은 필수입니다."
            );
        }

        if (request.durationDays() != 7 && request.durationDays() != 30) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "정지 기간은 7일 또는 30일만 선택할 수 있습니다."
            );
        }

        if (StringUtils.hasText(request.memo())
                && request.memo().trim().length() > 400) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "관리자 메모는 400자 이하여야 합니다."
            );
        }
    }

    private SuspensionState resolveSuspensionState(User user) {
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            return SuspensionState.notSuspended("탈퇴한 회원입니다.");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT
                    action_type AS actionType,
                    memo AS memo,
                    created_at AS createdAt
                FROM admin_actions
                WHERE target_type = 'USER'
                  AND target_id = :targetUserId
                  AND action_type IN ('SUSPEND_USER', 'ACTIVATE_USER')
                ORDER BY created_at DESC, admin_action_id DESC
                LIMIT 1
                """,
                new MapSqlParameterSource(
                        "targetUserId",
                        user.getUserId()
                )
        );

        if (rows.isEmpty()) {
            return user.getStatus() == UserStatus.SUSPENDED
                    ? SuspensionState.suspendedWithoutExpiry()
                    : SuspensionState.notSuspended(null);
        }

        Map<String, Object> latestAction = rows.get(0);
        String actionType = stringValue(latestAction.get("actionType"));

        if ("ACTIVATE_USER".equals(actionType)) {
            return SuspensionState.notSuspended(null);
        }

        LocalDateTime suspendedUntil = extractSuspensionUntil(
                stringValue(latestAction.get("memo"))
        );

        if (suspendedUntil == null) {
            return SuspensionState.suspendedWithoutExpiry();
        }

        if (!suspendedUntil.isAfter(LocalDateTime.now())) {
            return SuspensionState.notSuspended("정지 기간이 만료되었습니다.");
        }

        long remainingDays = Math.max(
                1,
                (long) Math.ceil(
                        Duration.between(
                                LocalDateTime.now(),
                                suspendedUntil
                        ).toMinutes() / 1440.0
                )
        );

        return new SuspensionState(
                true,
                suspendedUntil,
                remainingDays,
                "정지 기간이 남아 있습니다."
        );
    }

    private void insertAdminAction(
            Long adminId,
            Long targetUserId,
            String actionType,
            String memo
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO admin_actions (
                    admin_id,
                    target_type,
                    target_id,
                    action_type,
                    memo
                ) VALUES (
                    :adminId,
                    'USER',
                    :targetUserId,
                    :actionType,
                    :memo
                )
                """,
                new MapSqlParameterSource()
                        .addValue("adminId", adminId)
                        .addValue("targetUserId", targetUserId)
                        .addValue("actionType", actionType)
                        .addValue("memo", memo)
        );
    }

    private String buildSuspensionMemo(
            Integer durationDays,
            LocalDateTime suspendedUntil,
            String requestMemo
    ) {
        String baseMemo = SUSPENSION_UNTIL_PREFIX
                + MEMO_DATE_FORMATTER.format(suspendedUntil)
                + " | durationDays="
                + durationDays;

        if (!StringUtils.hasText(requestMemo)) {
            return baseMemo;
        }

        return baseMemo + " | " + requestMemo.trim();
    }

    private LocalDateTime extractSuspensionUntil(String memo) {
        if (!StringUtils.hasText(memo)
                || !memo.startsWith(SUSPENSION_UNTIL_PREFIX)) {
            return null;
        }

        String value = memo.substring(SUSPENSION_UNTIL_PREFIX.length())
                .split("\\|", 2)[0]
                .trim();

        try {
            return LocalDateTime.parse(value, MEMO_DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    public record SuspensionResponse(
            Long userId,
            String email,
            String nickname,
            boolean suspended,
            Integer durationDays,
            LocalDateTime suspendedAt,
            LocalDateTime suspendedUntil,
            String message
    ) {
    }

    public record SuspensionStatusResponse(
            Long userId,
            String email,
            String nickname,
            boolean suspended,
            LocalDateTime suspendedUntil,
            Long remainingDays,
            String message
    ) {
    }

    private record SuspensionState(
            boolean suspended,
            LocalDateTime suspendedUntil,
            Long remainingDays,
            String message
    ) {
        private static SuspensionState notSuspended(String message) {
            return new SuspensionState(false, null, null, message);
        }

        private static SuspensionState suspendedWithoutExpiry() {
            return new SuspensionState(
                    true,
                    null,
                    null,
                    "정지 기간 정보가 없는 정지 회원입니다."
            );
        }
    }
}
