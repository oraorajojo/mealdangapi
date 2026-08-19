package com.example.mealdangapi.board.dto.request;

import com.example.mealdangapi.board.entity.ReportReasonCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신고 요청 바디.
 *
 * 와이어프레임 — 레시피 상세의 [신고] 버튼 → 팝업에서 사유 선택 후 전송
 *
 * @NoArgsConstructor + @Setter가 필요한 이유:
 *   Jackson이 JSON을 객체로 변환할 때 기본 생성자로 만든 뒤 값을 채우기 때문.
 *   final 필드 + 생성자만 두면 역직렬화가 안 된다.
 */
@Getter
@Setter
@NoArgsConstructor
public class PostReportRequest {

    /**
     * 신고 사유. SPAM / ABUSE / ADULT / FALSE_INFO / COPYRIGHT / ETC
     *
     * 없는 값이 오면 Jackson이 역직렬화 단계에서 걸러낸다.
     */
    @NotNull(message = "신고 사유를 선택해주세요.")
    private ReportReasonCode reasonCode;

    /**
     * 기타 사유 상세. reasonCode가 ETC일 때만 필수다.
     *
     * "ETC일 때만 필수"라는 조건부 규칙은 @NotNull로 표현할 수 없어서
     * 서비스에서 검증한다(ErrorCode.REPORT_REASON_REQUIRED).
     * 여기서는 길이 제한만 건다. DB 컬럼이 VARCHAR(500)이라
     * 초과분을 미리 막지 않으면 DB까지 가서 예외가 난다.
     */
    @Size(max = 500, message = "기타 사유는 500자 이하로 입력해주세요.")
    private String etcReason;
}