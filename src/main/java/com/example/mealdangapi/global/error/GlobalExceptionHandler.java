package com.example.mealdangapi.global.error;

import com.example.mealdangapi.global.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러에서 터진 예외를 여기서 한 번에 응답으로 변환한다.
 *
 * 이게 있으면 컨트롤러/서비스에 try-catch를 안 써도 된다.
 * 서비스는 그냥 throw new BusinessException(...) 하고 끝내면 된다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 우리가 의도적으로 던진 예외.
     * 예상된 흐름이므로 stack trace는 남기지 않고 warn 한 줄만 찍는다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("[BusinessException] {} - {}", code.getCode(), e.getMessage());
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.fail(code.getCode(), e.getMessage()));
    }

    /**
     * @Valid 검증 실패 (@RequestBody DTO).
     * 어느 필드가 왜 틀렸는지까지 내려줘야 프론트가 폼에 표시할 수 있다.
     * 예: 신고 사유 500자 초과, 별점 누락
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        FieldError first = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String message = (first == null)
                ? ErrorCode.INVALID_INPUT.getMessage()
                : first.getField() + ": " + first.getDefaultMessage();

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    /** @Validated 검증 실패 (@RequestParam, @PathVariable) */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), e.getMessage()));
    }

    /**
     * DB 제약 위반 (UNIQUE, CHECK, FK).
     *
     * 원칙은 서비스에서 미리 검사해서 BusinessException으로 막는 것이지만,
     * 동시 요청으로 검사와 INSERT 사이를 비집고 들어오는 경우가 반드시 생긴다.
     *   예) 같은 사용자가 좋아요 버튼을 빠르게 두 번 → 둘 다 "없음" 판정 후 둘 다 INSERT
     * 그때 500이 나가지 않도록 최소한 409로 받아준다.
     *
     * 어떤 제약이 걸렸는지 메시지 파싱으로 구분하는 건 DB 버전에 따라 깨지므로 하지 않는다.
     * 원인 추적은 로그로 하고, 사용자에게는 뭉뚱그린 문구를 내린다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("[DataIntegrityViolation] {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(org.springframework.http.HttpStatus.CONFLICT)
                .body(ApiResponse.fail("DATA_CONFLICT", "요청이 중복되었거나 처리할 수 없는 데이터입니다."));
    }

    /**
     * 예상 못 한 모든 예외.
     * 여기는 반드시 stack trace를 남긴다. 안 그러면 원인 추적이 불가능하다.
     * 사용자에게는 내부 메시지를 노출하지 않는다(예외 메시지에 테이블명·쿼리가 섞여 나갈 수 있음).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("[Unexpected] {}", e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}