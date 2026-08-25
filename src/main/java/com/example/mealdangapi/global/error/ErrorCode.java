package com.example.mealdangapi.global.error;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 한 곳 모음.
 *
 * 여기 한 파일에 3명 몫이 다 모이면 병합 충돌이 잦아지므로,
 * 도메인별 구획을 나눠 두고 각자 자기 구획에만 줄을 추가하는 규칙으로 쓴다.
 * (같은 파일이라도 서로 다른 줄에 추가하면 git이 대부분 자동 병합한다)
 *
 * status → 이 에러가 나갈 때의 HTTP 상태코드
 * code   → 프론트 분기용 식별자. 절대 바꾸지 않는다(바꾸면 프론트가 깨진다)
 * message→ 사용자 노출 문구. 이건 자유롭게 다듬어도 된다
 */
public enum ErrorCode {

    // ─── 공통 ──────────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),

    // ─── 회원/로그인 ───────────────────────────────────────
    // 정지·탈퇴 계정의 로그인 실패를 "이메일/비밀번호 오류"와 구분해야
    // 프론트가 "정지된 계정입니다" 안내를 정확히 띄울 수 있다.
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "정지된 회원입니다."),
    ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN, "ACCOUNT_WITHDRAWN", "탈퇴한 회원입니다."),

    // ─── 게시판 (종선) ─────────────────────────────────────
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
    // status가 HIDDEN/DELETED인 글에 접근한 경우. NOT_FOUND와 구분해야
    // 프론트에서 "신고로 숨김 처리된 글입니다" 안내를 띄울 수 있다.
    POST_NOT_VISIBLE(HttpStatus.NOT_FOUND, "POST_NOT_VISIBLE", "비공개 처리된 게시글입니다."),
    // uq_board_posts_recipe(UNIQUE) 위반. 레시피 1개당 게시글 1개 제약.
    POST_ALREADY_EXISTS(HttpStatus.CONFLICT, "POST_ALREADY_EXISTS", "이미 게시글이 등록된 레시피입니다."),
    // 본인이 작성한 게시글이 아닌데 삭제를 시도한 경우
    POST_NOT_OWNED(HttpStatus.FORBIDDEN, "POST_NOT_OWNED", "본인이 작성한 게시글만 삭제할 수 있습니다."),

    // ─── 좋아요 (종선) ─────────────────────────────────────
    // PK(user_id, post_id) 중복. 규칙사전 §12 "회원당 게시글 1회만"
    ALREADY_LIKED(HttpStatus.CONFLICT, "ALREADY_LIKED", "이미 좋아요한 게시글입니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE_NOT_FOUND", "좋아요 기록이 없습니다."),

    // ─── 신고 (종선) ───────────────────────────────────────
    // uq_post_reports_post_user(UNIQUE) 위반
    ALREADY_REPORTED(HttpStatus.CONFLICT, "ALREADY_REPORTED", "이미 신고한 게시글입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "신고 내역을 찾을 수 없습니다."),
    // reason_code='ETC'인데 etc_reason이 비어 있는 경우
    REPORT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "REPORT_REASON_REQUIRED", "기타 사유를 입력해주세요."),
    // 이미 ACCEPTED/DISMISSED로 처리된 신고를 다시 처리하려는 경우
    REPORT_ALREADY_HANDLED(HttpStatus.CONFLICT, "REPORT_ALREADY_HANDLED", "이미 처리된 신고입니다."),

    // ─── 후기 (종선) ───────────────────────────────────────
    // uq_reviews_user_recipe(UNIQUE) 위반. 규칙사전 §12 "회원당 레시피 1개만 작성"
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 후기를 작성한 레시피입니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "후기를 찾을 수 없습니다."),
    // chk_reviews_rating CHECK(1~5) 위반을 DB까지 보내지 않고 앞단에서 막는다
    INVALID_RATING(HttpStatus.BAD_REQUEST, "INVALID_RATING", "별점은 1~5점만 입력할 수 있습니다."),
    REVIEW_NOT_OWNED(HttpStatus.FORBIDDEN, "REVIEW_NOT_OWNED", "본인이 작성한 후기만 수정할 수 있습니다."),

    // ─── 냉장고 (종선) ─────────────────────────────────────
    FRIDGE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIDGE_ITEM_NOT_FOUND", "등록된 재료를 찾을 수 없습니다."),
    FRIDGE_ITEM_NOT_OWNED(HttpStatus.FORBIDDEN, "FRIDGE_ITEM_NOT_OWNED", "본인의 재료만 수정할 수 있습니다."),
    // 규칙사전 §4-3: 인식 못 한 재료명은 임의로 확정하지 않고 사용자에게 되묻는다.
    // ingredient_id가 FK RESTRICT라 표준 재료로 매핑되지 않으면 애초에 저장 자체가 불가능하다.
    INGREDIENT_NOT_RECOGNIZED(HttpStatus.BAD_REQUEST, "INGREDIENT_NOT_RECOGNIZED", "인식할 수 없는 재료입니다."),
    // chk_user_fridge_quantity CHECK(quantity IS NULL OR quantity > 0)
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "수량은 0보다 커야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
