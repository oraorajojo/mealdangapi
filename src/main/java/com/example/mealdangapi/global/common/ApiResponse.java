package com.example.mealdangapi.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 전 API 공통 응답 포맷.
 *
 * 왜 필요한가:
 *   컨트롤러마다 응답 모양이 다르면 프론트가 엔드포인트별로 파싱 코드를 따로 짜야 한다.
 *   3명이 각자 브랜치에서 개발하는 상황이라 이걸 안 맞추면 나중에 통합할 때 전부 손봐야 한다.
 *
 * 성공: { "success": true,  "data": { ... } }
 * 실패: { "success": false, "error": { "code": "POST_NOT_FOUND", "message": "..." } }
 *
 * @JsonInclude(NON_NULL) → 성공 응답에 error 키가, 실패 응답에 data 키가 안 나가도록 정리한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorBody error;

    private ApiResponse(boolean success, T data, ErrorBody error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /** 데이터가 있는 성공 응답 (조회, 생성 등) */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 반환할 데이터가 없는 성공 응답 (삭제, 좋아요 취소 등).
     * Void로 두면 호출부에서 제네릭 추론이 깔끔하다.
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * 실패 응답. 직접 부르지 말고 GlobalExceptionHandler를 통해서만 만들어진다.
     * 컨트롤러에서 이걸 직접 호출하기 시작하면 예외 처리가 다시 흩어진다.
     */
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorBody getError() {
        return error;
    }

    /**
     * 에러 상세. code는 프론트가 분기 처리할 때 쓰는 식별자(문자열 고정),
     * message는 사용자에게 그대로 보여줄 수 있는 한국어 문구.
     * → 프론트가 message 문자열을 비교해서 분기하는 일이 없도록 code를 반드시 같이 내려준다.
     */
    public static class ErrorBody {
        private final String code;
        private final String message;

        public ErrorBody(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
