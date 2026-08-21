package com.example.mealdangapi.board.service;

import com.example.mealdangapi.board.dto.response.BoardPostDetailResponse;
import com.example.mealdangapi.board.dto.response.BoardPostListItemResponse;
import com.example.mealdangapi.board.dto.response.BoardPostListRow;
import com.example.mealdangapi.board.entity.BoardPost;
import com.example.mealdangapi.board.entity.PostStatus;
import com.example.mealdangapi.board.repository.BoardPostRepository;
import com.example.mealdangapi.board.repository.PostLikeRepository;
import com.example.mealdangapi.board.repository.PostReportRepository;
import com.example.mealdangapi.global.common.PageResponse;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.review.repository.ReviewRepository;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 게시글 조회 서비스 (미식 연구소) — 담당: 종선
 *
 * ★ 목록 조회의 쿼리 구성 (총 5번)
 *   ① 게시글 + 레시피 조인 (필터·페이징 포함)
 *   ② 작성자 닉네임 일괄 조회
 *   ③ 시간대 배지 일괄 조회
 *   ④ 후기 수 일괄 조회
 *   ⑤ 좋아요 여부 일괄 조회 (비로그인이면 생략)
 *
 *   ②~⑤를 게시글마다 개별 조회하면 12개 목록에 쿼리가 48번 나간다(N+1).
 *   IN 절로 묶어 각 1번으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class BoardPostService {

    private final BoardPostRepository boardPostRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostReportRepository postReportRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /**
     * 게시판 목록 조회. 비로그인도 열람 가능하다.
     *
     * @param userId   로그인 사용자 ID. 비로그인이면 null
     * @param chefCode 셰프 필터. null이면 전체
     * @param mealTime 시간대 필터. null이면 전체
     */
    @Transactional(readOnly = true)
    public PageResponse<BoardPostListItemResponse> getPosts(
            Long userId,
            ChefCode chefCode,
            MealTime mealTime,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // ① 게시글 + 레시피 조인. PUBLISHED이면서 레시피가 활성인 것만.
        Page<BoardPostListRow> rows = boardPostRepository.findBoardList(
                PostStatus.PUBLISHED,
                chefCode,
                mealTime,
                pageable
        );

        // ★ 결과가 비면 여기서 끝낸다.
        //   아래 IN 절 쿼리에 빈 리스트를 넘기면 "IN ()"이 되어 SQL 문법 오류가 난다.
        if (rows.isEmpty()) {
            return PageResponse.from(rows.map(row -> (BoardPostListItemResponse) null));
        }

        List<Long> recipeIds = rows.getContent().stream()
                .map(BoardPostListRow::getRecipeId)
                .toList();

        List<Long> postIds = rows.getContent().stream()
                .map(BoardPostListRow::getPostId)
                .toList();

        List<Long> authorIds = rows.getContent().stream()
                .map(BoardPostListRow::getAuthorUserId)
                .distinct()
                .toList();

        // ②~⑤ 부가 정보를 각각 한 번의 쿼리로 가져온다
        Map<Long, String> nicknames = loadNicknames(authorIds);
        Map<Long, List<String>> mealTimesByRecipe = loadMealTimes(recipeIds);
        Map<Long, Long> reviewCountsByRecipe = loadReviewCounts(recipeIds);
        Set<Long> likedPostIds = loadLikedPostIds(userId, postIds);

        return PageResponse.from(rows, row ->
                BoardPostListItemResponse.of(
                        row,
                        nicknames.get(row.getAuthorUserId()),
                        mealTimesByRecipe.getOrDefault(row.getRecipeId(), List.of()),
                        reviewCountsByRecipe.getOrDefault(row.getRecipeId(), 0L),
                        likedPostIds.contains(row.getPostId())
                )
        );
    }

    /**
     * 게시글 상세 조회. 조회수를 1 증가시킨다.
     *
     * readOnly가 아닌 이유는 조회수 UPDATE가 발생하기 때문이다.
     */
    @Transactional
    public BoardPostDetailResponse getPostDetail(Long userId, Long postId) {
        // 조회수를 먼저 올린 뒤 조회한다.
        // 순서를 바꾸면 응답에 갱신 전 값이 나간다.
        boardPostRepository.increaseViewCount(postId);

        BoardPost post = boardPostRepository
                .findByPostIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        return toDetailResponse(post, userId);
    }

    /**
     * 레시피에 연결된 게시글 조회.
     * 레시피 상세 화면의 신고 버튼에 넘길 postId를 얻는 용도.
     */
    @Transactional(readOnly = true)
    public BoardPostDetailResponse getPostByRecipeId(Long userId, Long recipeId) {
        BoardPost post = boardPostRepository.findByRecipeId(recipeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.isVisible()) {
            throw new BusinessException(ErrorCode.POST_NOT_VISIBLE);
        }

        return toDetailResponse(post, userId);
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────

    private BoardPostDetailResponse toDetailResponse(BoardPost post, Long userId) {
        boolean liked = false;
        boolean reported = false;

        if (userId != null) {
            liked = postLikeRepository
                    .existsByUserIdAndPostId(userId, post.getPostId());
            reported = postReportRepository
                    .existsByPostIdAndReporterUserId(post.getPostId(), userId);
        }

        String authorNickname = userRepository.findById(post.getUserId())
                .map(User::getNickname)
                .orElse(null);

        return BoardPostDetailResponse.of(post, authorNickname, liked, reported);
    }

    /**
     * 작성자 닉네임을 한 번의 쿼리로 조회.
     *
     * 게시글마다 개별 조회하면 12개 목록에 쿼리가 12번 나간다(N+1).
     * 같은 사람이 여러 글을 쓴 경우도 있어 distinct 처리된 ID로 조회한다.
     */
    private Map<Long, String> loadNicknames(List<Long> authorIds) {
        if (authorIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> result = new HashMap<>();

        for (User user : userRepository.findAllById(authorIds)) {
            result.put(user.getUserId(), user.getNickname());
        }

        return result;
    }

    /**
     * 레시피별 추천 시간대를 Map으로 변환.
     * 레시피 1개가 여러 시간대를 가지므로 값이 List가 된다.
     */
    private Map<Long, List<String>> loadMealTimes(List<Long> recipeIds) {
        Map<Long, List<String>> result = new HashMap<>();

        for (Object[] row : boardPostRepository.findMealTimesByRecipeIds(recipeIds)) {
            Long recipeId = (Long) row[0];
            MealTime mealTime = (MealTime) row[1];

            result.computeIfAbsent(recipeId, key -> new ArrayList<>())
                    .add(mealTime.name());
        }

        return result;
    }

    /**
     * 레시피별 후기 수를 Map으로 변환.
     *
     * ※ 후기가 0건인 레시피는 결과에 포함되지 않는다(GROUP BY 특성).
     *   호출부에서 getOrDefault(recipeId, 0L)로 처리한다.
     */
    private Map<Long, Long> loadReviewCounts(List<Long> recipeIds) {
        Map<Long, Long> result = new HashMap<>();

        for (Object[] row : reviewRepository.countByRecipeIds(recipeIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }

        return result;
    }

    /** 비로그인이면 조회 자체를 생략한다 */
    private Set<Long> loadLikedPostIds(Long userId, List<Long> postIds) {
        if (userId == null || postIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(
                postLikeRepository.findLikedPostIds(userId, postIds)
        );
    }
}
