package com.example.mealdangapi.board.service;

import com.example.mealdangapi.board.dto.response.PostLikeResponse;
import com.example.mealdangapi.board.entity.BoardPost;
import com.example.mealdangapi.board.entity.PostLike;
import com.example.mealdangapi.board.entity.PostStatus;
import com.example.mealdangapi.board.repository.BoardPostRepository;
import com.example.mealdangapi.board.repository.PostLikeRepository;
import com.example.mealdangapi.global.error.BusinessException;
import com.example.mealdangapi.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좋아요 서비스 — 담당: 종선
 *
 * 규칙사전 §12 — "게시글 좋아요: 회원당 게시글 1회만 가능"
 *
 * ★ 핵심: POST_LIKES INSERT/DELETE와 BOARD_POSTS.like_count 증감이
 *   반드시 같은 트랜잭션이어야 한다. 갈라지면 실제 좋아요 수와 화면 숫자가 어긋난다.
 *   메서드 전체에 @Transactional을 걸어 두 작업을 하나로 묶는다.
 */
@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final BoardPostRepository boardPostRepository;

    /**
     * 좋아요 등록.
     */
    @Transactional
    public PostLikeResponse like(Long userId, Long postId) {
        BoardPost post = findVisiblePost(postId);

        // 중복 확인. DB PK(user_id, post_id)도 막아주지만,
        // 미리 확인해서 명확한 에러 코드(ALREADY_LIKED)를 내려주는 게 프론트에 친절하다.
        //
        // ※ 이 확인과 아래 INSERT 사이에 동시 요청이 끼어들면 PK 중복 예외가 발생한다.
        //   그 경우는 GlobalExceptionHandler의 DataIntegrityViolationException 핸들러가
        //   409로 받아주므로 500이 나가지 않는다.
        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        postLikeRepository.save(PostLike.of(userId, postId));

        // ★ 엔티티 setter가 아니라 원자적 UPDATE를 쓴다.
        //   post.setLikeCount(post.getLikeCount() + 1) 방식이면
        //   동시 요청 시 둘 다 같은 값을 읽어 lost update가 발생한다.
        boardPostRepository.increaseLikeCount(postId);

        // increaseLikeCount는 clearAutomatically = true라 영속성 컨텍스트를 비운다.
        // 따라서 위에서 읽은 post 객체의 likeCount는 갱신 전 값이다.
        // 정확한 값을 응답하려면 다시 조회해야 한다.
        int likeCount = getCurrentLikeCount(postId);

        return new PostLikeResponse(postId, true, likeCount);
    }

    /**
     * 좋아요 취소.
     */
    @Transactional
    public PostLikeResponse unlike(Long userId, Long postId) {
        BoardPost post = findVisiblePost(postId);

        // 삭제된 행 수로 판단한다. 조회 후 삭제하는 2단계 대신 1단계로 처리되고,
        // 동시에 두 번 취소 요청이 와도 두 번째는 0이 반환되어 걸러진다.
        int deleted = postLikeRepository.deleteByUserIdAndPostId(userId, postId);

        if (deleted == 0) {
            throw new BusinessException(ErrorCode.LIKE_NOT_FOUND);
        }

        // ★ decreaseLikeCount의 쿼리에는 WHERE likeCount > 0 조건이 걸려 있다.
        //   like_count가 INT UNSIGNED라 0에서 1을 빼면 MySQL strict mode에서
        //   예외가 발생하고 트랜잭션 전체가 롤백되기 때문이다.
        boardPostRepository.decreaseLikeCount(postId);

        int likeCount = getCurrentLikeCount(postId);

        return new PostLikeResponse(postId, false, likeCount);
    }

    /**
     * 노출 가능한 게시글인지 확인하고 반환.
     * 숨김·삭제된 글에는 좋아요를 누를 수 없다.
     */
    private BoardPost findVisiblePost(Long postId) {
        return boardPostRepository
                .findByPostIdAndStatus(postId, PostStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /** 갱신 후의 정확한 카운트를 다시 읽어온다 */
    private int getCurrentLikeCount(Long postId) {
        return boardPostRepository.findById(postId)
                .map(BoardPost::getLikeCount)
                .orElse(0);
    }
}