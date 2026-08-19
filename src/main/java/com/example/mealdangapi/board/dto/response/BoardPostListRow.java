package com.example.mealdangapi.board.dto.response;

import com.example.mealdangapi.recipe.entity.ChefCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 게시판 목록 조회 결과를 담는 중간 객체 (JPQL 프로젝션 전용)
 *
 * 왜 별도 클래스가 필요한가:
 *   JPQL의 생성자 표현식(new ...)은 조회한 컬럼만 넘길 수 있다.
 *   그런데 최종 응답에는 "내가 좋아요를 눌렀는지(liked)"가 필요한데,
 *   이 값은 별도 조회로 얻으므로 쿼리 결과에 포함될 수 없다.
 *
 *   그래서 쿼리 결과는 이 클래스로 받고,
 *   서비스에서 liked를 합쳐 BoardPostListItemResponse로 변환한다.
 *
 * ★ 생성자의 파라미터 순서와 타입은 JPQL의 new 표현식과 정확히 일치해야 한다.
 *   하나라도 어긋나면 기동 시점이 아니라 쿼리 실행 시점에 에러가 난다.
 */
@Getter
public class BoardPostListRow {

    // board_posts
    private final Long postId;
    private final Long recipeId;
    private final Long authorUserId;
    private final String title;
    private final int viewCount;
    private final int likeCount;
    private final LocalDateTime createdAt;

    // recipes (조인으로 가져오는 값)
    private final String recipeName;
    private final ChefCode chefCode;
    private final String imageUrl;
    private final Integer cookingTimeMin;
    private final BigDecimal annoyanceScore;

    public BoardPostListRow(
            Long postId,
            Long recipeId,
            Long authorUserId,
            String title,
            int viewCount,
            int likeCount,
            LocalDateTime createdAt,
            String recipeName,
            ChefCode chefCode,
            String imageUrl,
            Integer cookingTimeMin,
            BigDecimal annoyanceScore
    ) {
        this.postId = postId;
        this.recipeId = recipeId;
        this.authorUserId = authorUserId;
        this.title = title;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.recipeName = recipeName;
        this.chefCode = chefCode;
        this.imageUrl = imageUrl;
        this.cookingTimeMin = cookingTimeMin;
        this.annoyanceScore = annoyanceScore;
    }
}
