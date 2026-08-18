package com.example.mealdangapi.recommend.dto;

/** React -> Spring 요청 (POST /api/recommend/{recommendLogId}/select)
 *  userId는 요청 본문이 아니라 JWT(Authentication)에서 추출한다. */
public record SelectRequest(Long recipeId) {
}
