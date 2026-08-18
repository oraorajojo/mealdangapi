package com.example.mealdangapi.board.repository;

import com.example.mealdangapi.board.entity.PostLike;
import com.example.mealdangapi.board.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 좋아요 저장소 (POST_LIKES) — 담당: 종선
 *
 * ★ 식별자 타입이 Long이 아니라 PostLikeId다.
 *   JpaRepository<PostLike, PostLikeId>
 *   복합키라서 findById()에 PostLikeId 객체를 넘겨야 한다.
 *     postLikeRepository.findById(new PostLikeId(userId, postId))
 *
 * 규칙사전 §12 — "게시글 좋아요: 회원당 게시글 1회만 가능"
 *   PK가 (user_id, post_id)라 DB가 중복을 직접 막아준다.
 */
public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    /**
     * 이미 좋아요를 눌렀는지 확인.
     *
     * existsById(new PostLikeId(userId, postId))로도 되지만,
     * 이름을 붙여두면 호출부가 읽기 쉽다.
     */
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    /**
     * 좋아요 취소.
     *
     * @return 삭제된 행 수. 0이면 애초에 좋아요가 없었다는 뜻이다.
     *         이 값으로 판단하면 "조회 후 삭제" 2단계를 1단계로 줄일 수 있고,
     *         동시 요청으로 두 번 취소되는 경우도 걸러진다.
     *         (0이면 like_count를 깎지 않아야 카운트가 어긋나지 않는다)
     *
     * ※ deleteBy 계열은 @Transactional 안에서 호출해야 한다.
     *   서비스 메서드에 @Transactional이 붙어 있으면 된다.
     */
    int deleteByUserIdAndPostId(Long userId, Long postId);

    /**
     * 특정 게시글의 좋아요 수를 실제로 세어본다.
     *
     * 평소에는 board_posts.like_count 캐시 값을 쓰고 이건 안 쓴다.
     * 캐시가 어긋났을 때 대조하거나 복구용으로 쓰는 메서드다.
     */
    long countByPostId(Long postId);

    /**
     * 내가 좋아요한 게시글 ID 목록 (주어진 후보 중에서).
     *
     * 게시판 목록에서 하트를 채워 보여줄 때 쓴다.
     * 게시글 12개마다 좋아요 여부를 하나씩 조회하면 쿼리가 12번 나간다(N+1 문제).
     * 화면에 뿌릴 postId를 한 번에 넘겨 조회하고 Set으로 만들어 대조하면 쿼리 1번으로 끝난다.
     *
     * ※ 이건 쿼리 메서드로 만들 수 없다. 반환할 값이 엔티티가 아니라 특정 컬럼이라
     *   @Query로 직접 써줘야 한다.
     */
    @Query("SELECT pl.postId FROM PostLike pl "
            + "WHERE pl.userId = :userId AND pl.postId IN :postIds")
    List<Long> findLikedPostIds(@Param("userId") Long userId,
                                @Param("postIds") List<Long> postIds);
}
