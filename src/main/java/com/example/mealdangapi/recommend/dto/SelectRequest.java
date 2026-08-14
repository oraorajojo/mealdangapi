package com.example.mealdangapi.recommend.dto;

/** React -> Spring 요청 (POST /api/recommend/{recommendLogId}/select)
 *  userId는 로그인 연동 전 임시 필드. 인증이 붙으면 요청 본문이 아니라 SecurityContext에서 꺼내야 한다. */
public record SelectRequest(Long recipeId, Long userId) {
}
