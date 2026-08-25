package com.example.mealdangapi.admin.dto;

import com.example.mealdangapi.recipe.entity.ChefCode;
import com.example.mealdangapi.recipe.entity.RecipeSourceType;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 "전체 게시판" 목록 조회 결과를 담는 중간 객체 (JPQL 프로젝션 전용).
 *
 * board_posts가 없는 레시피(공공 API·관리자 등록)도 함께 보여줘야 해서
 * BoardPostListRow처럼 board_posts INNER JOIN을 전제로 할 수 없다. 그래서
 * FROM Recipe를 기준으로 하고, postId/viewCount/likeCount는 연결된 게시글이
 * 없으면 null이 된다(스칼라 서브쿼리라 매칭되는 행이 없으면 SQL NULL).
 *
 * ★ 생성자의 파라미터 순서와 타입은 JPQL의 new 표현식과 정확히 일치해야 한다.
 */
@Getter
public class AdminPostRow {

    private final Long recipeId;
    private final Long postId;
    private final String title;
    private final String imageUrl;
    private final ChefCode chefCode;
    private final RecipeSourceType sourceType;
    private final Integer viewCount;
    private final Integer likeCount;
    private final LocalDateTime createdAt;

    public AdminPostRow(
            Long recipeId,
            Long postId,
            String title,
            String imageUrl,
            ChefCode chefCode,
            RecipeSourceType sourceType,
            Integer viewCount,
            Integer likeCount,
            LocalDateTime createdAt
    ) {
        this.recipeId = recipeId;
        this.postId = postId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.chefCode = chefCode;
        this.sourceType = sourceType;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
    }
}
