package com.example.mealdangapi.board.service;

import com.example.mealdangapi.board.api.BoardPostCommandApi;
import com.example.mealdangapi.board.entity.BoardPost;
import com.example.mealdangapi.board.repository.BoardPostRepository;
import com.example.mealdangapi.board.repository.PostReportRepository;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.recipe.entity.Recipe;
import com.example.mealdangapi.recipe.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * BoardPostCommandApi 구현체 — 치연 도메인이 호출하는 진입점.
 *
 * ★ 트랜잭션 규칙
 *   모든 메서드가 @Transactional 기본값(REQUIRED)이다.
 *   호출한 쪽(레시피 등록 / 관리자 신고 처리)의 트랜잭션에 그대로 참여한다.
 *   REQUIRES_NEW를 붙이면 트랜잭션이 분리돼 레시피는 롤백됐는데 게시글만 남는
 *   부분 반영이 생긴다. 협의자료 §4 위배.
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

    /**
     * [정식 시그니처] 협의자료 PDF §3 기준.
     */
    @Override
    @Transactional
    public Long createBoardPost(Long recipeId, Long authorUserId) {
        if (recipeId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "recipeId는 필수입니다."
            );
        }
        if (authorUserId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "authorUserId는 필수입니다."
            );
        }

        // recipe_id에 UNIQUE가 걸려 있어 중복 생성 시 DB에서 터진다.
        // 레시피 등록 트랜잭션 전체가 롤백되므로 미리 막는다.
        if (boardPostRepository.existsByRecipeId(recipeId)) {
            throw new BusinessException(ErrorCode.POST_ALREADY_EXISTS);
        }

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "존재하지 않는 레시피입니다. recipeId=" + recipeId
                ));

        BoardPost post = BoardPost.ofRecipe(
                authorUserId,
                recipeId,
                buildTitle(recipe),
                buildContent(recipe)
        );

        return boardPostRepository.save(post).getPostId();
    }

    /**
     * [호환용 오버로드] 카톡 협의 시그니처.
     * recipes.author_user_id를 역조회해 정식 메서드로 위임한다.
     */
    @Override
    @Transactional
    public Long createBoardPost(Long recipeId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "존재하지 않는 레시피입니다. recipeId=" + recipeId
                ));

        // ★ 공공 API(FOOD_SAFETY_API)나 관리자(ADMIN)가 등록한 레시피는
        //   author_user_id가 없다. board_posts.user_id는 NOT NULL이므로
        //   이 경로로는 게시글을 만들 수 없다.
        //
        //   애초에 "미식 연구소"는 회원이 직접 올린 레시피를 공유하는 게시판이므로
        //   USER_SUBMISSION만 게시글이 되는 게 맞다.
        //   → 호출하는 쪽(레시피 등록 서비스)에서 sourceType을 확인하고 호출해야 한다.
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
    //  신고 인정 → 게시글 숨김
    // ═══════════════════════════════════════════════════════════

    /**
     * [정식 시그니처] 신고 인정 시 게시글 숨김 처리.
     *
     * 이 메서드가 처리하는 것:
     *   ① POST_REPORTS.status = 'ACCEPTED' (해당 게시글의 PENDING 건 전체)
     *   ② BOARD_POSTS.status  = 'HIDDEN'
     *
     * 호출하는 쪽(치연)이 같은 트랜잭션에서 처리해야 하는 것:
     *   ③ RECIPES.is_active = FALSE  ← 빠뜨리면 게시판에서만 사라지고 추천에는 계속 노출됨
     *   ④ ADMIN_ACTIONS INSERT
     */
    @Override
    @Transactional
    public void hidePost(Long postId, Long adminId) {
        BoardPost post = boardPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // ① 이 게시글의 PENDING 신고를 한 번에 ACCEPTED로 전환.
        //    협의 문서에 빠져 있던 항목이다. post_reports가 종선 테이블이므로
        //    여기서 처리한다. 치연 쪽에서 중복 구현할 필요 없다.
        //
        //    한 게시글에 신고가 10건 이상 쌓여 있으므로 엔티티를 하나씩 불러와
        //    accept()를 호출하면 UPDATE가 10번 나간다. 한 번의 UPDATE로 끝낸다.
        int accepted = postReportRepository
                .acceptAllPendingByPostId(postId, adminId, now);

        // ② 게시글 숨김
        post.hide(adminId);

        log.info(
                "게시글 숨김 처리 완료. postId={}, adminId={}, 처리된 신고={}건",
                postId, adminId, accepted
        );
    }

    /**
     * [호환용 오버로드] 카톡 협의 시그니처.
     * adminId가 없어 moderated_by_admin_id가 NULL로 남는다.
     */
    @Override
    @Transactional
    public void hidePost(Long postId) {
        log.warn(
                "adminId 없이 hidePost가 호출되었습니다. "
                        + "moderated_by_admin_id가 기록되지 않습니다. postId={}",
                postId
        );
        hidePost(postId, null);
    }

    // ═══════════════════════════════════════════════════════════
    //  title / content 생성
    // ═══════════════════════════════════════════════════════════

    /**
     * 게시글 제목 = 레시피 이름.
     *
     * recipes.name은 VARCHAR(200) NOT NULL,
     * board_posts.title도 VARCHAR(200) NOT NULL이라 길이가 그대로 맞는다.
     */
    private String buildTitle(Recipe recipe) {
        return recipe.getName();
    }

    /**
     * 게시글 본문 = 레시피 한 줄 소개.
     *
     * ★ 협의가 필요했던 지점:
     *   recipes.summary는 NULL 허용인데 board_posts.content는 NOT NULL이다.
     *   한 줄 소개 없이 등록한 레시피가 들어오면 NOT NULL 위반으로 예외가 발생하고,
     *   같은 트랜잭션이라 레시피 저장까지 통째로 롤백된다.
     *
     *   레시피 등록이 실패하는 것보다 기본 문구를 채우는 편이 사용자 경험상 낫다고 판단해
     *   서버에서 대체 문구를 생성한다.
     *   (치연과 협의해 시그니처에 content를 추가하기로 하면 이 로직은 제거)
     */
    private String buildContent(Recipe recipe) {
        if (StringUtils.hasText(recipe.getSummary())) {
            return recipe.getSummary();
        }

        return recipe.getName() + " 레시피입니다.";
    }
}