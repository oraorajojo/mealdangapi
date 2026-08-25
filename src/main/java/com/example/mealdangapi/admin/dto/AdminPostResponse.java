package com.example.mealdangapi.admin.dto;

import java.time.LocalDateTime;

/**
 * 관리자 "전체 게시판" 목록의 행 하나.
 *
 * postId가 null이면 게시글이 없는 레시피(공공 API·관리자 등록)라는 뜻이다.
 * 프론트는 이 값으로 삭제 시 어느 API를 호출할지 정한다
 * (postId 있으면 게시글 삭제, 없으면 레시피 비활성화만).
 */
public record AdminPostResponse(
        Long recipeId,
        Long postId,
        String title,
        String imageUrl,
        String chefCode,
        String sourceType,
        long viewCount,
        long likeCount,
        long reviewCount,
        LocalDateTime createdAt
) {
    public static AdminPostResponse of(AdminPostRow row, long reviewCount) {
        return new AdminPostResponse(
                row.getRecipeId(),
                row.getPostId(),
                row.getTitle(),
                row.getImageUrl(),
                row.getChefCode() != null ? row.getChefCode().name() : null,
                row.getSourceType() != null ? row.getSourceType().name() : null,
                row.getViewCount() != null ? row.getViewCount() : 0,
                row.getLikeCount() != null ? row.getLikeCount() : 0,
                reviewCount,
                row.getCreatedAt()
        );
    }
}
