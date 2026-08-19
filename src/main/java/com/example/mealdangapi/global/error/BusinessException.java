package com.example.mealdangapi.global.error;

/**
 * 비즈니스 규칙 위반 예외.
 *
 * RuntimeException을 상속한 이유:
 *   checked exception으로 만들면 서비스 메서드마다 throws가 붙고,
 *   무엇보다 @Transactional은 기본적으로 unchecked(RuntimeException)에서만 롤백한다.
 *   checked로 만들면 예외가 터져도 트랜잭션이 커밋돼서 데이터가 반쯤 저장되는 사고가 난다.
 *   → 좋아요 INSERT는 됐는데 like_count는 안 오른 상태 같은 것.
 *
 * 사용:
 *   throw new BusinessException(ErrorCode.POST_NOT_FOUND);
 *   throw new BusinessException(ErrorCode.INVALID_INPUT, "제목은 200자를 넘을 수 없습니다.");
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        // 기본 메시지를 RuntimeException의 message로도 넘겨두면 로그에 그대로 찍혀서 디버깅이 편하다
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 상황별로 더 구체적인 문구를 내려야 할 때 사용 */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
