package com.example.mealdangapi.recipe.repository;

import com.example.mealdangapi.admin.dto.AdminPostRow;
import com.example.mealdangapi.board.entity.PostStatus;
import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.MealTime;
import com.example.mealdangapi.recipe.entity.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends
        JpaRepository<Recipe, Long>,
        JpaSpecificationExecutor<Recipe> {

    Optional<Recipe> findByRecipeIdAndActiveTrue(Long recipeId);

    @Query("""
            SELECT r
            FROM Recipe r
            WHERE r.active = true
              AND r.chefCode = :chefCode
              AND r.annoyanceScore >= :minScore
              AND r.annoyanceScore < :maxScore
              AND EXISTS (
                  SELECT 1
                  FROM RecipeMeal rm
                  WHERE rm.recipe = r
                    AND rm.id.mealTime = :mealTime
              )
            ORDER BY r.recipeId ASC
            """)
    List<Recipe> findActiveCandidateRecipes(
            @Param("chefCode") ChefCode chefCode,
            @Param("minScore") BigDecimal minScore,
            @Param("maxScore") BigDecimal maxScore,
            @Param("mealTime") MealTime mealTime
    );

    /**
     * 관리자 "전체 게시판" — 회원 등록 게시글과 공공 API·관리자 등록 레시피를
     * 전부 합쳐서 보여준다. FROM Recipe 기준이라 board_posts가 아예 없는
     * 레시피(공공 API 등)도 빠지지 않는다.
     *
     * postId/viewCount/likeCount는 스칼라 서브쿼리로 가져온다. 연결된 게시글이
     * 없으면 서브쿼리가 행을 못 찾아 NULL이 되는데, LEFT JOIN보다 이 방식이
     * board_posts에 실제 연관관계(@ManyToOne)가 없는 이 코드베이스 구조에서
     * 더 안전하다(엔티티 간 임의 ON 조인 없이 값만 조회).
     *
     * ★ 작성자 본인 삭제(deleteBySelf)는 board_posts.status만 DELETED로 바꾸고
     *   recipes.is_active는 그대로 둔다(설계상 의도적 분리). 그래서 r.active만
     *   보면 삭제된 게시글의 레시피가 계속 활성으로 잡혀 이 목록에 남아버린다.
     *   연결된 게시글이 DELETED면 통째로 제외한다. 게시글이 없거나(공공 API)
     *   PUBLISHED/HIDDEN이면 그대로 보여준다.
     *
     * countQuery를 직접 지정한 이유는 findBoardList와 동일 — 생성자 표현식이
     * 들어간 쿼리는 Spring Data가 count 쿼리를 자동으로 만들지 못한다.
     */
    @Query(
            value = "SELECT new com.example.mealdangapi.admin.dto.AdminPostRow("
                    + "  r.recipeId, "
                    + "  (SELECT p.postId FROM BoardPost p WHERE p.recipeId = r.recipeId), "
                    + "  r.name, r.imageUrl, r.chefCode, r.sourceType, "
                    + "  (SELECT p.viewCount FROM BoardPost p WHERE p.recipeId = r.recipeId), "
                    + "  (SELECT p.likeCount FROM BoardPost p WHERE p.recipeId = r.recipeId), "
                    + "  r.createdAt) "
                    + "FROM Recipe r "
                    + "WHERE r.active = true "
                    + "  AND NOT EXISTS ("
                    + "        SELECT 1 FROM BoardPost p "
                    + "        WHERE p.recipeId = r.recipeId AND p.status = :deletedStatus) "
                    + "ORDER BY r.recipeId DESC",
            countQuery = "SELECT COUNT(r) FROM Recipe r "
                    + "WHERE r.active = true "
                    + "  AND NOT EXISTS ("
                    + "        SELECT 1 FROM BoardPost p "
                    + "        WHERE p.recipeId = r.recipeId AND p.status = :deletedStatus)"
    )
    Page<AdminPostRow> findAllForAdmin(
            @Param("deletedStatus") PostStatus deletedStatus,
            Pageable pageable
    );
}
