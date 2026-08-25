package com.example.mealdangapi.board.repository;

import com.example.mealdangapi.board.dto.response.BoardPostListRow;
import com.example.mealdangapi.board.entity.BoardPost;
import com.example.mealdangapi.board.entity.PostStatus;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 게시글 저장소 (BOARD_POSTS) — 담당: 종선
 */
public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    // ═══════════════════════════════════════════════════════════
    //  게시판 목록 — 레시피 조인 + 필터
    // ═══════════════════════════════════════════════════════════

    /**
     * 미식 연구소 목록 조회. 셰프·시간대 필터를 지원한다.
     *
     * ★ 왜 조인이 필요한가
     *   와이어프레임의 필터(한식/중식/양식/기타, 아침/점심/저녁/야식)와
     *   카드에 표시할 대표이미지·조리시간·귀찮음지수는 모두 recipes 쪽에 있다.
     *   board_posts에는 이 값들이 없어서 조인 없이는 화면을 구성할 수 없다.
     *
     * ★ FROM BoardPost p, Recipe r WHERE p.recipeId = r.recipeId
     *   BoardPost가 Recipe를 @ManyToOne으로 매핑하지 않고 Long FK로만 들고 있어서
     *   (치연 브랜치와의 병합 충돌을 피하려는 의도적 설계)
     *   JOIN 문법 대신 이렇게 조건으로 연결한다. 결과는 INNER JOIN과 같다.
     *
     * ★ r.active = true 조건
     *   신고 인정으로 비활성화된 레시피는 게시글도 HIDDEN이 되지만,
     *   레시피만 따로 비활성화되는 경로(작성자가 직접 비활성화)도 있어
     *   두 조건을 모두 확인해야 한다.
     *
     * ★ (:chefCode IS NULL OR ...) 패턴
     *   파라미터가 null이면 그 조건을 무시한다. 필터 조합마다 메서드를
     *   따로 만들지 않아도 되게 하는 방식이다.
     *
     * ★ 시간대는 EXISTS 서브쿼리를 쓴다
     *   recipe_meals는 레시피 1개당 여러 행(아침+저녁 등)이라
     *   직접 조인하면 같은 게시글이 여러 번 나온다(중복 행).
     *   EXISTS는 "조건에 맞는 행이 하나라도 있는가"만 보므로 중복이 생기지 않는다.
     *
     * ★ 재료 필터(hasKeywords/keywords)도 같은 이유로 EXISTS를 쓴다. 레시피 1개가
     *   재료를 여러 개 가지므로 recipe_ingredients를 직접 조인하면 중복 행이 생긴다.
     *   "필터 없음"을 ":keywords IS EMPTY"로 확인하지 않는 이유: JPQL의 IS EMPTY는
     *   엔티티의 컬렉션 연관관계 경로(o.items 같은)에만 쓸 수 있고 바인드 파라미터엔
     *   못 쓴다. 대신 boolean 플래그(hasKeywords)를 따로 받아 분기한다.
     *
     * ★ countQuery를 직접 지정한 이유
     *   생성자 표현식(new ...)이 들어간 쿼리는 Spring Data가 count 쿼리를
     *   자동으로 만들어내지 못한다. 페이징을 쓰려면 반드시 따로 적어줘야 한다.
     */
    @Query(
            value = "SELECT new com.example.mealdangapi.board.dto.response.BoardPostListRow("
                    + "  p.postId, p.recipeId, p.userId, p.title, "
                    + "  p.viewCount, p.likeCount, p.createdAt, "
                    + "  r.name, r.chefCode, r.imageUrl, r.cookingTimeMin, r.annoyanceScore) "
                    + "FROM BoardPost p, Recipe r "
                    + "WHERE p.recipeId = r.recipeId "
                    + "  AND p.status = :status "
                    + "  AND r.active = true "
                    + "  AND (:chefCode IS NULL OR r.chefCode = :chefCode) "
                    + "  AND (:mealTime IS NULL OR EXISTS ("
                    + "        SELECT 1 FROM RecipeMeal rm "
                    + "        WHERE rm.id.recipeId = r.recipeId "
                    + "          AND rm.id.mealTime = :mealTime)) "
                    + "  AND (:hasKeywords = false OR EXISTS ("
                    + "        SELECT 1 FROM RecipeIngredient ri "
                    + "        WHERE ri.recipe.recipeId = r.recipeId "
                    + "          AND LOWER(ri.ingredient.name) IN :keywords)) ",
            countQuery = "SELECT COUNT(p) "
                    + "FROM BoardPost p, Recipe r "
                    + "WHERE p.recipeId = r.recipeId "
                    + "  AND p.status = :status "
                    + "  AND r.active = true "
                    + "  AND (:chefCode IS NULL OR r.chefCode = :chefCode) "
                    + "  AND (:mealTime IS NULL OR EXISTS ("
                    + "        SELECT 1 FROM RecipeMeal rm "
                    + "        WHERE rm.id.recipeId = r.recipeId "
                    + "          AND rm.id.mealTime = :mealTime)) "
                    + "  AND (:hasKeywords = false OR EXISTS ("
                    + "        SELECT 1 FROM RecipeIngredient ri "
                    + "        WHERE ri.recipe.recipeId = r.recipeId "
                    + "          AND LOWER(ri.ingredient.name) IN :keywords)) "
    )
    Page<BoardPostListRow> findBoardList(
            @Param("status") PostStatus status,
            @Param("chefCode") ChefCode chefCode,
            @Param("mealTime") MealTime mealTime,
            @Param("hasKeywords") boolean hasKeywords,
            @Param("keywords") List<String> keywords,
            Pageable pageable
    );

    /**
     * 여러 레시피의 추천 시간대를 한 번에 조회. (카드 배지용)
     *
     * 게시글마다 개별 조회하면 N+1이 발생하므로 IN 절로 묶는다.
     *   [0] = recipeId (Long)
     *   [1] = mealTime (MealTime)
     * 레시피 1개가 여러 시간대를 가지므로 같은 recipeId가 여러 번 나온다.
     */
    @Query("SELECT rm.id.recipeId, rm.id.mealTime FROM RecipeMeal rm "
            + "WHERE rm.id.recipeId IN :recipeIds")
    List<Object[]> findMealTimesByRecipeIds(@Param("recipeIds") List<Long> recipeIds);

    // ═══════════════════════════════════════════════════════════
    //  단건 조회
    // ═══════════════════════════════════════════════════════════

    /** 노출 가능한 게시글만 조회. HIDDEN·DELETED는 걸러진다 */
    Optional<BoardPost> findByPostIdAndStatus(Long postId, PostStatus status);

    /** 레시피에 연결된 게시글. recipe_id가 UNIQUE라 결과는 0 또는 1개 */
    Optional<BoardPost> findByRecipeId(Long recipeId);

    /** createBoardPost 중복 생성 방지 */
    boolean existsByRecipeId(Long recipeId);

    /**
     * 관리자 페이지 — 신고 누적 게시글.
     * 임계값 10은 하드코딩하지 않고 파라미터로 받는다.
     */
    Page<BoardPost> findAllByReportCountGreaterThanEqualAndStatus(
            int threshold, PostStatus status, Pageable pageable);

    // ═══════════════════════════════════════════════════════════
    //  카운트 갱신 — 원자적 UPDATE
    // ═══════════════════════════════════════════════════════════
    //
    // ★ 엔티티 setter를 쓰지 않는 이유
    //   "읽고 → 더하고 → 쓰기" 3단계로 하면 동시 요청 시
    //   둘 다 같은 값을 읽고 같은 값을 써서 한 번만 반영된다(lost update).
    //   DB에게 "현재값 + 1"을 시키면 행 잠금으로 순차 처리된다.
    //
    // ★ @Modifying(clearAutomatically = true)
    //   실행 후 영속성 컨텍스트를 비운다. 안 그러면 같은 트랜잭션에서
    //   다시 조회했을 때 캐시된 갱신 전 값이 나온다.

    @Modifying(clearAutomatically = true)
    @Query("UPDATE BoardPost p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    int increaseViewCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE BoardPost p SET p.likeCount = p.likeCount + 1 WHERE p.postId = :postId")
    int increaseLikeCount(@Param("postId") Long postId);

    /**
     * ★ WHERE likeCount > 0 조건 필수.
     *   like_count가 INT UNSIGNED라 0에서 1을 빼면 MySQL strict mode에서
     *   예외가 발생하며 트랜잭션 전체가 롤백된다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BoardPost p SET p.likeCount = p.likeCount - 1 "
            + "WHERE p.postId = :postId AND p.likeCount > 0")
    int decreaseLikeCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE BoardPost p SET p.reportCount = p.reportCount + 1 WHERE p.postId = :postId")
    int increaseReportCount(@Param("postId") Long postId);
}
