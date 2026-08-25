package com.example.mealdangapi.admin.service;

import com.example.mealdangapi.admin.dto.AdminPostResponse;
import com.example.mealdangapi.admin.dto.AdminPostRow;
import com.example.mealdangapi.board.entity.PostStatus;
import com.example.mealdangapi.global.common.PageResponse;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import com.example.mealdangapi.review.repository.ReviewRepository;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.entity.UserRole;
import com.example.mealdangapi.user.entity.UserStatus;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 "전체 게시판" — 회원 게시글 + 공공 API·관리자 등록 레시피를 한 목록에서
 * 훑어보고, 필요하면 레시피 상세로 들어가 수정·삭제할 수 있게 한다.
 * (실제 수정/삭제는 기존 RecipeCommandService/BoardPostService가 관리자
 * 예외를 이미 처리하므로, 여기는 조회만 담당한다)
 */
@Service
@RequiredArgsConstructor
public class AdminPostService {

    private static final int PAGE_SIZE_MIN = 1;
    private static final int PAGE_SIZE_MAX = 50;

    private final RecipeRepository recipeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<AdminPostResponse> getAllPosts(
            String adminEmail,
            int page,
            int size
    ) {
        requireActiveAdmin(adminEmail);
        validateListParameters(page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<AdminPostRow> rows = recipeRepository.findAllForAdmin(PostStatus.DELETED, pageable);

        if (rows.isEmpty()) {
            return PageResponse.from(rows.map(row -> (AdminPostResponse) null));
        }

        List<Long> recipeIds = rows.getContent().stream()
                .map(AdminPostRow::getRecipeId)
                .toList();

        Map<Long, Long> reviewCounts = new HashMap<>();
        for (Object[] row : reviewRepository.countByRecipeIds(recipeIds)) {
            reviewCounts.put((Long) row[0], (Long) row[1]);
        }

        return PageResponse.from(rows, row ->
                AdminPostResponse.of(
                        row,
                        reviewCounts.getOrDefault(row.getRecipeId(), 0L)
                )
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
                    "활성 상태의 관리자만 조회할 수 있습니다."
            );
        }

        return admin;
    }

    private void validateListParameters(int page, int size) {
        if (page < 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "page는 0 이상이어야 합니다."
            );
        }

        if (size < PAGE_SIZE_MIN || size > PAGE_SIZE_MAX) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "size는 1~50 사이여야 합니다."
            );
        }
    }
}
