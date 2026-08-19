package com.example.mealdangapi.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시판 목록 카드 1개 (미식 연구소 화면)
 *
 * 와이어프레임 카드 구성:
 *   대표이미지 / 셰프·시간대 배지 / 레시피명 / 한 줄 소개 / 🚪조회 ♥좋아요 📄리뷰
 */
@Getter
@AllArgsConstructor
public class BoardPostListItemResponse {

    private Long postId;
    private Long recipeId;
    private Long authorUserId;

    /** 게시글 제목 (= 레시피명) */
    private String title;

    /** 카드 배지용. KOREAN / CHINESE / WESTERN / ETC */
    private String chefCode;

    /** 카드 배지용. BREAKFAST / LUNCH / DINNER / LATE_NIGHT (복수 가능) */
    private List<String> mealTimes;

    /** 대표 이미지. 없으면 null이므로 프론트에서 기본 이미지 처리 필요 */
    private String imageUrl;

    private Integer cookingTimeMin;

    /** 귀찮음 지수 1.00~5.00 */
    private BigDecimal annoyanceScore;

    private int viewCount;
    private int likeCount;

    /** 카드의 📄 숫자. 해당 레시피에 달린 후기 수 */
    private long reviewCount;

    /** 로그인 사용자의 좋아요 여부. 비로그인이면 항상 false */
    private boolean liked;

    private LocalDateTime createdAt;

    public static BoardPostListItemResponse of(
            BoardPostListRow row,
            List<String> mealTimes,
            long reviewCount,
            boolean liked
    ) {
        return new BoardPostListItemResponse(
                row.getPostId(),
                row.getRecipeId(),
                row.getAuthorUserId(),
                row.getTitle(),
                row.getChefCode() != null ? row.getChefCode().name() : null,
                mealTimes,
                row.getImageUrl(),
                row.getCookingTimeMin(),
                row.getAnnoyanceScore(),
                row.getViewCount(),
                row.getLikeCount(),
                reviewCount,
                liked,
                row.getCreatedAt()
        );
    }
}
