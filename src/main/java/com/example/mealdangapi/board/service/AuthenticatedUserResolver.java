package com.example.mealdangapi.board.service;

import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import com.example.mealdangapi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 정보(이메일)로부터 user_id를 얻는 헬퍼.
 *
 * 왜 별도 클래스로 뺐는가:
 *   게시글 조회·좋아요·신고 서비스 세 곳에서 모두 필요한 로직이라
 *   각자 UserRepository를 주입받고 같은 코드를 반복하는 대신 한 곳에 모았다.
 *
 * ※ 담당 원칙과의 관계
 *   "타 담당 테이블을 직접 조작하지 않는다"는 협의 원칙이 있지만,
 *   이건 쓰기가 아니라 인증 주체를 식별하기 위한 읽기 전용 조회다.
 *   치연의 RecipeCommandService도 동일하게 userRepository.findByEmail()을 사용한다.
 *
 * ※ Spring Security 구조상 인증 주체가 이메일(username)로 들어오기 때문에
 *   user_id를 쓰려면 이 변환이 반드시 한 번 필요하다.
 *   나중에 JWT에 userId를 직접 담는 방식으로 바뀌면 이 클래스만 고치면 된다.
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    /**
     * 인증된 사용자의 user_id를 반환한다. 로그인이 필요한 API에서 사용.
     *
     * @param email Authentication.getName()으로 얻은 이메일
     * @throws BusinessException 미인증이거나 사용자를 찾을 수 없는 경우 401
     */
    @Transactional(readOnly = true)
    public Long requireUserId(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED))
                .getUserId();
    }

    /**
     * 인증된 사용자의 user_id를 반환하되, 비로그인이면 null을 반환한다.
     *
     * 게시판 목록·상세는 비로그인도 열람 가능하지만,
     * 로그인 상태라면 "내가 좋아요한 글"을 표시해야 하므로 이 구분이 필요하다.
     */
    @Transactional(readOnly = true)
    public Long resolveUserIdOrNull(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return userRepository.findByEmail(email)
                .map(user -> user.getUserId())
                .orElse(null);
    }
}