package com.example.mealdangapi.review.service;

import com.example.mealdangapi.global.common.PageResponse;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import com.example.mealdangapi.recommend.ChefSelectionRepository;
import com.example.mealdangapi.review.dto.request.ReviewRequest;
import com.example.mealdangapi.review.dto.response.ReviewResponse;
import com.example.mealdangapi.review.dto.response.ReviewSummaryResponse;
import com.example.mealdangapi.review.entity.Review;
import com.example.mealdangapi.review.repository.ReviewRepository;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 후기 서비스 (REVIEWS) — 담당: 종선
 *
 * 규칙사전 §12
 *   · 후기: 회원당 레시피 1개만 작성, 수정 가능
 *   · 별점: 1~5점 정수만 허용
 *
 * ★ 후기는 게시글이 아니라 레시피에 달린다.
 *   와이어프레임에서도 레시피 상세 화면 하단에 위치한다.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final ChefSelectionRepository chefSelectionRepository;

    /**
     * 레시피의 후기 목록 (최신순). 비로그인도 열람 가능하다.
     *
     * @param userId 로그인 사용자 ID. 비로그인이면 null (mine 판정용)
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviews(
            Long userId,
            Long recipeId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Review> reviews = reviewRepository
                .findAllByRecipeIdOrderByCreatedAtDesc(recipeId, pageable);

        // 작성자 닉네임을 한 번에 조회한다.
        // 후기마다 개별 조회하면 10개 목록에 쿼리가 10번 나간다(N+1).
        Map<Long, String> nicknames = loadNicknames(reviews.getContent());

        return PageResponse.from(reviews, review ->
                ReviewResponse.of(
                        review,
                        nicknames.get(review.getUserId()),
                        userId != null && review.isWrittenBy(userId)
                )
        );
    }

    /**
     * 레시피 상세 상단의 후기 요약 (평점, 개수).
     */
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long userId, Long recipeId) {
        long count = reviewRepository.countByRecipeId(recipeId);

        // 후기가 없으면 AVG가 null을 반환한다. Double(래퍼)로 받아야 NPE가 안 난다.
        Double average = reviewRepository.findAverageRating(recipeId);

        Double rounded = (average == null)
                ? null
                : BigDecimal.valueOf(average)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        Long myReviewId = null;

        if (userId != null) {
            myReviewId = reviewRepository
                    .findByUserIdAndRecipeId(userId, recipeId)
                    .map(Review::getReviewId)
                    .orElse(null);
        }

        long selectionCount = chefSelectionRepository.countByRecipeId(recipeId);

        return new ReviewSummaryResponse(
                recipeId,
                rounded,
                count,
                myReviewId != null,
                myReviewId,
                count + selectionCount
        );
    }

    /**
     * 후기 작성.
     *
     * 규칙사전 §12 — 회원당 레시피 1개만 작성 가능.
     * DB에도 UNIQUE (user_id, recipe_id)가 걸려 있지만,
     * 미리 확인해 명확한 에러 코드를 내려준다.
     */
    @Transactional
    public ReviewResponse createReview(
            Long userId,
            Long recipeId,
            ReviewRequest request
    ) {
        // reviews.recipe_id가 FK라 존재하지 않는 레시피면 DB에서 터진다.
        // 먼저 확인해 의미 있는 메시지를 내려준다.
        if (!recipeRepository.existsById(recipeId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "존재하지 않는 레시피입니다."
            );
        }

        if (reviewRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = reviewRepository.save(
                Review.of(userId, recipeId, request.getRating(), request.getContent())
        );

        return ReviewResponse.of(review, findNickname(userId), true);
    }

    /**
     * 후기 수정. 본인이 작성한 것만 수정 가능하다.
     */
    @Transactional
    public ReviewResponse updateReview(
            Long userId,
            Long reviewId,
            ReviewRequest request
    ) {
        Review review = findOwnedReview(userId, reviewId);

        review.update(request.getRating(), request.getContent());

        // 변경 감지(dirty checking)로 트랜잭션 종료 시 UPDATE가 실행된다.
        // save()를 명시적으로 부르지 않아도 된다.

        return ReviewResponse.of(review, findNickname(userId), true);
    }

    /**
     * 후기 삭제. 본인이 작성한 것만 삭제 가능하다.
     *
     * 후기는 게시글과 달리 물리 삭제한다.
     * 운영 이력으로 보존할 필요가 없고, 삭제 후 같은 레시피에 다시 쓸 수 있어야 하는데
     * 소프트 삭제로 두면 UNIQUE (user_id, recipe_id)에 걸려 재작성이 막힌다.
     */
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = findOwnedReview(userId, reviewId);
        reviewRepository.delete(review);
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────

    /** 존재하면서 본인이 작성한 후기인지 확인 */
    private Review findOwnedReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_OWNED);
        }

        return review;
    }

    /** 후기 목록의 작성자 닉네임을 한 번의 쿼리로 조회 */
    private Map<Long, String> loadNicknames(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return Map.of();
        }

        List<Long> userIds = reviews.stream()
                .map(Review::getUserId)
                .distinct()
                .toList();

        Map<Long, String> result = new HashMap<>();

        for (User user : userRepository.findAllById(userIds)) {
            result.put(user.getUserId(), user.getNickname());
        }

        return result;
    }

    private String findNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElse(null);
    }
}
