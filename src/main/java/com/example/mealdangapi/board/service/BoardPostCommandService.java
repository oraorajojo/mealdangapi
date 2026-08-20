package com.example.mealdangapi.board.service;

import com.example.mealdangapi.board.api.BoardPostCommandApi;
import com.example.mealdangapi.board.dto.response.ReportedPostResponse;
import com.example.mealdangapi.board.dto.response.ReportedPostRow;
import com.example.mealdangapi.board.entity.BoardPost;
import com.example.mealdangapi.board.entity.ReportReasonCode;
import com.example.mealdangapi.board.repository.BoardPostRepository;
import com.example.mealdangapi.board.repository.PostReportRepository;
import com.example.mealdangapi.global.common.PageResponse;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BoardPostCommandApi 구현체 — 치연 도메인이 호출하는 진입점.
 *
 * ★ 트랜잭션 규칙
 *   모든 메서드가 @Transactional 기본값(REQUIRED)이다.
 *   호출한 쪽(레시피 등록 / 관리자 신고 처리)의 트랜잭션에 그대로 참여한다.
 *   REQUIRES_NEW를 붙이면 트랜잭션이 분리돼 부분 반영이 생긴다. 협의자료 §4 위배.
 */
@Service
@RequiredArgsConstructor
public class BoardPostCommandService implements BoardPostCommandApi {

    private static final Logger log =
            LoggerFactory.getLogger(BoardPostCommandService.class);

    private final BoardPostRepository boardPostRepository;
    private final PostReportRepository postReportRepository;
    private final RecipeRepository recipeRepository;

    // ═══════════════════════════════════════════════════════════
    //  레시피 등록 → 게시글 자동 생성
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public Long createBoardPost(Long recipeId, Long authorUserId) {
        if (recipeId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "recipeId는 필수입니다.");
        }
        if (authorUserId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "authorUserId는 필수입니다.");
        }

        // recipe_id에 UNIQUE가 걸려 있어 중복 생성 시 DB에서 터진다.
        // 레시피 등록 트랜잭션 전체가 롤백되므로 미리 막는다.
        if (boardPostRepository.existsByRecipeId(recipeId)) {
            throw new BusinessException(ErrorCode.POST_ALREADY_EXISTS);
        }

        Recipe recipe = findRecipe(recipeId);

        BoardPost post = BoardPost.ofRecipe(
                authorUserId,
                recipeId,
                buildTitle(recipe),
                buildContent(recipe)
        );

        return boardPostRepository.save(post).getPostId();
    }

    @Override
    @Transactional
    public Long createBoardPost(Long recipeId) {
        Recipe recipe = findRecipe(recipeId);

        // 공공 API(FOOD_SAFETY_API)·관리자(ADMIN) 등록 레시피는 author가 없다.
        // board_posts.user_id는 NOT NULL이라 게시글을 만들 수 없다.
        if (recipe.getAuthor() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "작성자가 없는 레시피는 게시글을 만들 수 없습니다. "
                            + "회원이 등록한 레시피(USER_SUBMISSION)만 대상입니다."
            );
        }

        return createBoardPost(recipeId, recipe.getAuthor().getUserId());
    }

    // ═══════════════════════════════════════════════════════════
    //  관리자 신고 처리
    // ═══════════════════════════════════════════════════════════

    /**
     * 신고 수락 — 게시글 숨김.
     *
     * 반환값(recipeId)으로 치연 측이 recipes.is_active = FALSE를 처리한다.
     */
    @Override
    @Transactional
    public Long hidePost(Long postId, Long adminId) {
        BoardPost post = boardPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // ① 이 게시글의 PENDING 신고를 일괄 ACCEPTED로 전환.
        //    post_reports가 게시판 테이블이므로 여기서 처리한다.
        //    치연 쪽에서 중복 구현할 필요 없다.
        int accepted = postReportRepository
                .acceptAllPendingByPostId(postId, adminId, now);

        // ② 게시글 숨김
        post.hide(adminId);

        log.info("게시글 숨김 처리. postId={}, adminId={}, 처리 신고={}건, recipeId={}",
                postId, adminId, accepted, post.getRecipeId());

        // ③ 연결 레시피 ID 반환 → 호출 측에서 is_active = FALSE 처리
        //    recipe_id가 ON DELETE SET NULL이라 null일 수 있다.
        return post.getRecipeId();
    }

    @Override
    @Transactional
    public Long hidePost(Long postId) {
        log.warn("adminId 없이 hidePost가 호출되었습니다. "
                + "moderated_by_admin_id가 기록되지 않습니다. postId={}", postId);
        return hidePost(postId, null);
    }

    /**
     * 신고 기각 — PENDING 신고를 DISMISSED로 전환.
     *
     * 게시글 상태와 연결 레시피는 건드리지 않는다.
     */
    @Override
    @Transactional
    public int dismissPendingReports(Long postId, Long adminId) {
        // 존재하지 않는 게시글이면 명확히 알려준다.
        // 아래 UPDATE는 대상이 없어도 조용히 0을 반환하므로,
        // 이 확인이 없으면 "게시글이 없음"과 "처리할 신고가 없음"이 구분되지 않는다.
        if (!boardPostRepository.existsById(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        int dismissed = postReportRepository
                .dismissAllPendingByPostId(postId, adminId, LocalDateTime.now());

        log.info("신고 기각 처리. postId={}, adminId={}, 기각 신고={}건",
                postId, adminId, dismissed);

        // 0이어도 예외를 던지지 않는다.
        // 다른 관리자가 먼저 처리한 경우인데, 결과적으로 원하는 상태(PENDING 없음)가
        // 되어 있으므로 실패로 볼 이유가 없다.
        // 호출 측에서 필요하면 반환값으로 판단하면 된다.
        return dismissed;
    }

    /**
     * 관리자 신고 목록 조회.
     *
     * 쿼리 2번으로 처리한다.
     *   ① 게시글 + 레시피 조인 + PENDING 신고 건수/최근 시각 (페이징)
     *   ② 해당 게시글들의 사유별 집계 (IN 절로 일괄)
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportedPostResponse> getReportedPosts(
            int minReportCount, int page, int size
    ) {
        // 정렬은 쿼리에 ORDER BY로 박혀 있어 Pageable에 Sort를 넣지 않는다.
        // 양쪽에 정렬이 있으면 JPQL 문법 오류가 난다.
        Page<ReportedPostRow> rows = postReportRepository.findReportedPosts(
                minReportCount,
                PageRequest.of(page, size)
        );

        if (rows.isEmpty()) {
            // IN 절에 빈 리스트를 넘기면 "IN ()"이 되어 SQL 문법 오류가 난다.
            return PageResponse.from(rows.map(row -> (ReportedPostResponse) null));
        }

        List<Long> postIds = rows.getContent().stream()
                .map(ReportedPostRow::getPostId)
                .toList();

        Map<Long, List<ReportedPostResponse.ReasonCount>> reasonsByPost =
                loadReasons(postIds);

        return PageResponse.from(rows, row ->
                ReportedPostResponse.of(
                        row,
                        reasonsByPost.getOrDefault(row.getPostId(), List.of())
                )
        );
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────

    private Recipe findRecipe(Long recipeId) {
        return recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "존재하지 않는 레시피입니다. recipeId=" + recipeId
                ));
    }

    /** 게시글별 신고 사유 집계를 Map으로 변환 */
    private Map<Long, List<ReportedPostResponse.ReasonCount>> loadReasons(
            List<Long> postIds
    ) {
        Map<Long, List<ReportedPostResponse.ReasonCount>> result = new HashMap<>();

        for (Object[] row : postReportRepository.countReasonsByPostIds(postIds)) {
            Long postId = (Long) row[0];
            ReportReasonCode reasonCode = (ReportReasonCode) row[1];
            Long count = (Long) row[2];

            result.computeIfAbsent(postId, key -> new ArrayList<>())
                    .add(new ReportedPostResponse.ReasonCount(reasonCode.name(), count));
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════
    //  title / content 생성
    // ═══════════════════════════════════════════════════════════

    /** 게시글 제목 = 레시피 이름. 양쪽 다 VARCHAR(200)이라 길이가 맞는다 */
    private String buildTitle(Recipe recipe) {
        return recipe.getName();
    }

    /**
     * 게시글 본문 = 레시피 한 줄 소개.
     *
     * recipes.summary는 NULL 허용인데 board_posts.content는 NOT NULL이다.
     * 한 줄 소개 없이 등록한 레시피가 들어오면 NOT NULL 위반으로 예외가 발생하고,
     * 같은 트랜잭션이라 레시피 저장까지 롤백된다.
     *
     * 레시피 등록이 실패하는 것보다 기본 문구를 채우는 편이 낫다고 판단했다.
     */
    private String buildContent(Recipe recipe) {
        if (StringUtils.hasText(recipe.getSummary())) {
            return recipe.getSummary();
        }
        return recipe.getName() + " 레시피입니다.";
    }
}
