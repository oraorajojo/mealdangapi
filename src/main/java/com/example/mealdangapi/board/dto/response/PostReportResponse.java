package com.example.mealdangapi.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 신고 접수 결과.
 *
 * reportCount는 관리자 화면용 값이지만, 접수 확인 응답으로 함께 내려준다.
 * (일반 사용자 화면에 노출할지는 프론트 판단)
 */
@Getter
@AllArgsConstructor
public class PostReportResponse {

    private Long reportId;
    private Long postId;
    private int reportCount;
    private String message;
}