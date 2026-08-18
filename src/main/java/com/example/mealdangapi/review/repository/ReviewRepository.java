package com.example.mealdangapi.review.repository;

import com.example.mealdangapi.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 레시피 상세의 후기 목록 (최신순) */
    Page<Review> findAllByRecipeIdOrderByCreatedAtDesc(Long recipeId, Pageable pageable);

    /** 내가 이 레시피에 쓴 후기. 규칙사전 §12 "회원당 레시피 1개만" */
    Optional<Review> findByUserIdAndRecipeId(Long userId, Long recipeId);

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    /** 레시피 상세의 "조리 완료 27회" 표시용 */
    long countByRecipeId(Long recipeId);

    /**
     * 레시피 상세의 "후기 평점 4.6" 표시용.
     *
     * 후기가 하나도 없으면 AVG가 null을 반환하므로 Double(래퍼)로 받는다.
     * int로 받으면 언박싱 시점에 NPE가 난다.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.recipeId = :recipeId")
    Double findAverageRating(@Param("recipeId") Long recipeId);

    /**
     * 여러 레시피의 후기 수를 한 번에 조회. (게시판 목록 카드의 📄 숫자)
     *
     * ★ 게시글마다 countByRecipeId를 호출하면 12개 목록에 쿼리가 12번 나간다(N+1).
     *   IN 절로 한 번에 가져와 Map으로 만들어 쓴다.
     *
     * 반환 형태는 Object[] 배열의 리스트다. 각 배열의
     *   [0] = recipeId (Long)
     *   [1] = 후기 수 (Long)
     *
     * ※ 후기가 0건인 레시피는 결과에 아예 포함되지 않는다(GROUP BY 특성).
     *   따라서 서비스에서 Map으로 변환한 뒤 getOrDefault(recipeId, 0L)로 처리해야 한다.
     */
    @Query("SELECT r.recipeId, COUNT(r) FROM Review r "
            + "WHERE r.recipeId IN :recipeIds GROUP BY r.recipeId")
    List<Object[]> countByRecipeIds(@Param("recipeIds") List<Long> recipeIds);
}
