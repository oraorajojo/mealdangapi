package com.example.mealdangapi.board.dto.response;

import com.example.mealdangapi.board.entity.BoardPost;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 상세
 *
 * 게시글은 레시피와 1:1이므로, 화면에서는 이 응답의 recipeId로
 * 레시피 상세 API(GET /api/recipes/{recipeId})를 함께 호출해
 * 재료·조리단계 등을 가져오는 구조를 전제로 한다.
 */
@Getter
@AllArgsConstructor
public class BoardPostDetailResponse {

    private Long postId;
    private Long recipeId;
    private Long authorUserId;
    private String title;
    private String content;
    private int viewCount;
    private int likeCount;

    /** 로그인 사용자의 좋아요 여부 (비로그인 false) */
    private boolean liked;

    /**
     * 로그인 사용자가 이미 신고한 글인지.
     * 신고 버튼을 비활성화할지 판단하는 값. 중복 신고는 서버에서도 막지만,
     * 미리 알려주면 사용자가 눌렀다가 에러를 받는 일이 없다.
     */
    private boolean reported;

    private LocalDateTime createdAt;

    public static BoardPostDetailResponse of(
            BoardPost post,
            boolean liked,
            boolean reported
    ) {
        return new BoardPostDetailResponse(
                post.getPostId(),
                post.getRecipeId(),
                post.getUserId(),
                post.getTitle(),
                post.getContent(),
                post.getViewCount(),
                post.getLikeCount(),
                liked,
                reported,
                post.getCreatedAt()
        );
    }
}