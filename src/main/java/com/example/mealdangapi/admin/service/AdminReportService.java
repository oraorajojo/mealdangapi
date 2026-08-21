package com.example.mealdangapi.admin.service;

import com.example.mealdangapi.board.api.BoardPostCommandApi;
import com.example.mealdangapi.board.dto.response.ReportedPostResponse;
import com.example.mealdangapi.global.common.PageResponse;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
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

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final int MIN_REPORT_COUNT_MIN = 1;
    private static final int MIN_REPORT_COUNT_MAX = 100;
    private static final int PAGE_SIZE_MIN = 1;
    private static final int PAGE_SIZE_MAX = 100;
    private static final int MEMO_MAX_LENGTH = 400;

    private final BoardPostCommandApi boardPostCommandApi;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public PageResponse<ReportedPostResponse> getReportedPosts(
            String adminEmail,
            int minReportCount,
            int page,
            int size
    ) {
        requireActiveAdmin(adminEmail);
        validateListParameters(minReportCount, page, size);

        return boardPostCommandApi.getReportedPosts(
                minReportCount,
                page,
                size
        );
    }

    /**
     * 하나의 트랜잭션에서 아래 순서로 처리한다.
     * 1. 종선: PENDING 신고 전체 ACCEPTED, 게시글 HIDDEN
     * 2. 치연: 연결 레시피 is_active = false
     * 3. 치연: admin_actions 감사 로그 INSERT
     */
    @Transactional
    public ReportActionResponse acceptReport(
            String adminEmail,
            Long postId,
            String memo
    ) {
        User admin = requireActiveAdmin(adminEmail);
        validatePostId(postId);
        validateMemo(memo);

        Long recipeId = boardPostCommandApi.hidePost(
                postId,
                admin.getUserId()
        );

        boolean recipeDeactivated = false;

        if (recipeId != null) {
            Recipe recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INVALID_INPUT,
                            "게시글에 연결된 레시피를 찾을 수 없습니다."
                    ));

            recipe.deactivate();
            recipeDeactivated = true;
        }

        insertAdminAction(
                admin.getUserId(),
                postId,
                "ACCEPT_REPORT",
                buildMemo("신고 수락", memo)
        );

        return new ReportActionResponse(
                postId,
                recipeId,
                "ACCEPTED",
                null,
                recipeDeactivated,
                "신고를 수락하고 게시글을 숨김 처리했습니다."
        );
    }

    /**
     * 하나의 트랜잭션에서 아래 순서로 처리한다.
     * 1. 종선: PENDING 신고 전체 DISMISSED
     * 2. 치연: admin_actions 감사 로그 INSERT
     * 게시글 상태와 레시피 활성 상태는 변경하지 않는다.
     */
    @Transactional
    public ReportActionResponse dismissReport(
            String adminEmail,
            Long postId,
            String memo
    ) {
        User admin = requireActiveAdmin(adminEmail);
        validatePostId(postId);
        validateMemo(memo);

        int dismissedCount = boardPostCommandApi.dismissPendingReports(
                postId,
                admin.getUserId()
        );

        insertAdminAction(
                admin.getUserId(),
                postId,
                "DISMISS_REPORT",
                buildMemo(
                        "신고 기각 / 처리 건수=" + dismissedCount,
                        memo
                )
        );

        return new ReportActionResponse(
                postId,
                null,
                "DISMISSED",
                dismissedCount,
                false,
                dismissedCount == 0
                        ? "이미 처리된 신고이거나 처리할 미처리 신고가 없습니다."
                        : "신고를 기각 처리했습니다."
        );
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

    private void validateListParameters(
            int minReportCount,
            int page,
            int size
    ) {
        if (minReportCount < MIN_REPORT_COUNT_MIN
                || minReportCount > MIN_REPORT_COUNT_MAX) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "minReportCount는 1~100 사이여야 합니다."
            );
        }

        if (page < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "page는 0 이상이어야 합니다."
            );
        }

        if (size < PAGE_SIZE_MIN || size > PAGE_SIZE_MAX) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "size는 1~100 사이여야 합니다."
            );
        }
    }

    private void validatePostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "postId는 1 이상의 값이어야 합니다."
            );
        }
    }

    private void validateMemo(String memo) {
        if (StringUtils.hasText(memo)
                && memo.trim().length() > MEMO_MAX_LENGTH) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "관리자 메모는 400자 이하여야 합니다."
            );
        }
    }

    private void insertAdminAction(
            Long adminId,
            Long postId,
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
                    'BOARD_POST',
                    :postId,
                    :actionType,
                    :memo
                )
                """,
                new MapSqlParameterSource()
                        .addValue("adminId", adminId)
                        .addValue("postId", postId)
                        .addValue("actionType", actionType)
                        .addValue("memo", memo)
        );
    }

    private String buildMemo(String actionDescription, String requestMemo) {
        if (!StringUtils.hasText(requestMemo)) {
            return actionDescription;
        }

        return actionDescription + " | " + requestMemo.trim();
    }

    public record ReportActionResponse(
            Long postId,
            Long recipeId,
            String reportStatus,
            Integer handledReportCount,
            boolean recipeDeactivated,
            String message
    ) {
    }
}
