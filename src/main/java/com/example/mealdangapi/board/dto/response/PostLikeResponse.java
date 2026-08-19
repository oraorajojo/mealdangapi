package com.example.mealdangapi.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 좋아요 등록/취소 결과.
 *
 * 갱신된 카운트를 함께 돌려주는 이유:
 *   프론트가 좋아요를 누른 뒤 목록을 다시 조회하지 않아도 숫자를 바로 반영할 수 있다.
 *   프론트에서 임의로 +1 하는 방식은 서버 값과 어긋날 수 있어 서버 값을 그대로 내려준다.
 */
@Getter
@AllArgsConstructor
public class PostLikeResponse {

    private Long postId;
    private boolean liked;
    private int likeCount;
}