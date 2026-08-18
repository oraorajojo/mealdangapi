package com.example.mealdangapi.board.repository;

import com.example.mealdangapi.board.entity.BoardPost;
import com.example.mealdangapi.board.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 게시글 저장소 (BOARD_POSTS) — 담당: 종선
 *
 * JpaRepository<엔티티, 식별자타입>을 상속하면
 * save(), findById(), delete(), count() 같은 기본 메서드가 자동으로 생긴다.
 * 여기에는 그걸로 안 되는 것만 추가로 정의한다.
 *
 * ※ 셰프/시간대 필터, 리뷰 수 집계는 recipes·recipe_meals·reviews 조인이 필요해서
 *   치연 레시피 기능이 붙은 뒤에 추가한다. 지금은 board_posts 단독으로 되는 것만.
 */
public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    // ═══════════════════════════════════════════════════════════
    //  조회
    // ═══════════════════════════════════════════════════════════

    /**
     * 게시판 목록 — 미식 연구소 화면.
     *
     * 메서드 이름만으로 쿼리가 만들어진다(쿼리 메서드).
     * findAll + By + Status  →  WHERE status = ?
     * 정렬·페이징은 Pageable로 넘긴다:
     *   PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "createdAt"))
     *
     * status=PUBLISHED만 조회하는 게 핵심이다.
     * HIDDEN(신고 인정)·DELETED는 일반 사용자에게 보이면 안 된다.
     *
     * 인덱스: idx_board_posts_status_created (status, created_at)가 그대로 탄다.
     */
    Page<BoardPost> findAllByStatus(PostStatus status, Pageable pageable);

    /**
     * 게시글 단건 조회 — 노출 가능한 것만.
     *
     * findById()를 쓰면 HIDDEN 글도 그대로 반환돼서 서비스에서 매번 상태를 확인해야 한다.
     * 조회 단계에서 걸러두면 그 실수를 막을 수 있다.
     */
    Optional<BoardPost> findByPostIdAndStatus(Long postId, PostStatus status);

    /**
     * 레시피에 연결된 게시글 조회.
     *
     * 쓰이는 곳:
     *   - 레시피 상세 화면에서 신고 버튼에 넘길 postId를 찾을 때
     *   - createBoardPost() 중복 생성 방지 확인
     *
     * recipe_id에 UNIQUE가 걸려 있어 결과는 0개 아니면 1개다.
     */
    Optional<BoardPost> findByRecipeId(Long recipeId);

    /** 레시피에 이미 게시글이 있는지 (createBoardPost 중복 방지) */
    boolean existsByRecipeId(Long recipeId);

    /**
     * 관리자 페이지 — 신고 누적 게시글 목록.
     *
     * 확정 사항: 신고 10건 누적 시 관리자 검토 대상이 된다.
     * 임계값 10은 하드코딩하지 않고 파라미터로 받는다.
     * (application.yml에 mealdang.report.threshold: 10 으로 빼두면 조정이 쉽다)
     *
     * 인덱스: idx_board_posts_report (report_count, status)
     */
    Page<BoardPost> findAllByReportCountGreaterThanEqualAndStatus(
            int threshold, PostStatus status, Pageable pageable);

    // ═══════════════════════════════════════════════════════════
    //  카운트 갱신 — 원자적 UPDATE
    // ═══════════════════════════════════════════════════════════
    //
    // ★ 왜 엔티티 setter를 안 쓰고 이렇게 하는가
    //
    //   엔티티로 하면 "읽고 → 더하고 → 쓰기" 3단계가 된다.
    //     post.setLikeCount(post.getLikeCount() + 1);
    //   두 사람이 동시에 좋아요를 누르면 둘 다 5를 읽고 둘 다 6을 쓴다.
    //   실제로는 2번 눌렸는데 결과는 6. 이걸 lost update라고 한다.
    //
    //   아래처럼 DB에게 "현재값 + 1"을 시키면 DB가 행 잠금을 걸고 순서대로 처리한다.
    //   설계정의서 §12 검수 항목 "좋아요·신고 동시 요청 시 카운트 불일치 없는지"가 이것.
    //
    // ★ @Modifying 필수
    //   이게 없으면 Spring Data가 SELECT로 취급해서 실행 시점에 예외가 난다.
    //   clearAutomatically = true → 실행 후 영속성 컨텍스트를 비운다.
    //     안 그러면 같은 트랜잭션에서 다시 조회했을 때 캐시된 옛날 값이 나온다.
    //
    // ★ @Transactional은 여기 붙이지 않는다
    //   호출하는 서비스 메서드의 트랜잭션에 참여해야 하기 때문.
    //   좋아요 INSERT와 카운트 증가가 같은 트랜잭션이어야 한다.

    /**
     * 조회수 +1.
     *
     * ※ 알아둘 것: 이 UPDATE는 Hibernate를 거치지 않으므로
     *   DB의 ON UPDATE CURRENT_TIMESTAMP가 발동해 updated_at도 함께 갱신된다.
     *   즉 "조회만 해도 수정일시가 바뀐다". 정렬은 created_at 기준이라 지금은 무해하다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BoardPost p SET p.viewCount = p.viewCount + 1 WHERE p.postId = :postId")
    int increaseViewCount(@Param("postId") Long postId);

    /** 좋아요 +1. POST_LIKES INSERT와 같은 트랜잭션에서 호출할 것 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BoardPost p SET p.likeCount = p.likeCount + 1 WHERE p.postId = :postId")
    int increaseLikeCount(@Param("postId") Long postId);

    /**
     * 좋아요 -1.
     *
     * ★ WHERE에 likeCount > 0 조건이 반드시 필요하다.
     *   like_count가 INT UNSIGNED라서 0에서 1을 빼면 음수가 되는데,
     *   MySQL strict mode에서는 그 순간 에러가 나며 트랜잭션이 통째로 롤백된다.
     *   조건을 걸면 0일 때는 UPDATE 대상이 없어 반환값 0으로 조용히 넘어간다.
     *
     * @return 갱신된 행 수. 0이면 이미 0이었다는 뜻
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BoardPost p SET p.likeCount = p.likeCount - 1 "
            + "WHERE p.postId = :postId AND p.likeCount > 0")
    int decreaseLikeCount(@Param("postId") Long postId);

    /** 신고 +1. POST_REPORTS INSERT와 같은 트랜잭션에서 호출할 것 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BoardPost p SET p.reportCount = p.reportCount + 1 WHERE p.postId = :postId")
    int increaseReportCount(@Param("postId") Long postId);
}
