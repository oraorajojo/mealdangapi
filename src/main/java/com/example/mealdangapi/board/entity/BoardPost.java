package com.example.mealdangapi.board.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 커뮤니티 게시글 (BOARD_POSTS) — 담당: 종선
 *
 * 설계정의서 Rev.6 §5.4
 *   · 회원이 직접 글을 쓰는 공개 API는 없다. 레시피 등록 시 자동 생성만 존재한다.
 *   · recipe_id는 UNIQUE. 레시피 1개당 게시글 1개를 DB 레벨에서 강제.
 *   · report_count >= 10 이면 관리자 페이지에 노출된다.
 */
@Entity
@Table(name = "board_posts")
// ※ @Table에 indexes/uniqueConstraints를 적지 않은 이유:
//    이 속성들은 Hibernate가 DDL을 "생성"할 때만 쓰인다. 우리는 팀 공유 Railway DB에
//    이미 DDL을 적용해 뒀고 ddl-auto는 validate/none이므로 아무 효과가 없다.
//    오히려 실제 DB 인덱스와 어긋나면 나중에 읽는 사람이 헷갈린다.
//    실제 인덱스 목록은 밀당_테이블생성쿼리_전체.sql을 기준으로 본다.
public class BoardPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY = MySQL AUTO_INCREMENT.
    // 주의: IDENTITY는 persist() 시점에 INSERT를 즉시 날린다(쓰기 지연 배칭 불가).
    //       대신 save() 직후 postId가 바로 채워지므로, 레시피 등록 트랜잭션에서
    //       생성된 게시글 ID를 곧바로 반환할 수 있다. 우리 용도에는 이게 맞다.
    @Column(name = "post_id")
    private Long postId;

    /**
     * 작성자(= 레시피 등록 회원). FK → users.user_id, ON DELETE RESTRICT
     *
     * ★ @ManyToOne(User)로 매핑하지 않은 이유:
     *   User 엔티티는 치연 담당이고 아직 내 브랜치에 없다. 내가 지금 만들면
     *   병합할 때 User 엔티티가 두 벌이 되어 충돌한다. FK 값만 Long으로 들고 있으면
     *   내 브랜치는 독립적으로 컴파일/테스트가 되고, 나중에 필요해지면
     *   @ManyToOne(fetch = LAZY)로 승격하는 건 언제든 가능하다.
     *
     * updatable = false → 작성자는 한 번 정해지면 바뀌지 않는다.
     *   실수로 setter가 생기거나 dirty checking으로 UPDATE되는 걸 막는 안전장치.
     */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /**
     * 연결 레시피. FK → recipes.recipe_id, ON DELETE SET NULL, UNIQUE
     *
     * DB는 NULL을 허용하지만(레시피가 삭제되면 SET NULL로 남으므로),
     * 애플리케이션에서 새로 만들 때는 항상 값이 있어야 한다. (테이블정의서 비고)
     * → 생성자에서 requireNonNull로 강제한다.
     */
    @Column(name = "recipe_id", updatable = false)
    private Long recipeId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * columnDefinition = "TEXT"로 명시한 이유:
     *   @Lob을 붙이면 Hibernate 6가 CLOB으로 해석해 MySQL에서 longtext를 기대하는데,
     *   실제 DDL은 text다. ddl-auto=validate일 때 타입 불일치로 기동이 막힌다.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * ★ @Enumerated(EnumType.STRING) 필수.
     *   기본값인 ORDINAL로 두면 enum 선언 순서(0,1,2)가 숫자로 저장된다.
     *   DB 컬럼은 ENUM('PUBLISHED','HIDDEN','DELETED')이라 애초에 안 맞고,
     *   설령 맞더라도 나중에 enum 중간에 값을 추가하는 순간 기존 데이터가 전부 밀린다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostStatus status = PostStatus.PUBLISHED;

    // ─── 집계 캐시 컬럼 3종 (설계정의서 §8.2 반정규화) ───────────────
    // 원본 이벤트는 POST_LIKES / POST_REPORTS에 있고, 이건 목록 화면에서
    // 매번 COUNT(*) 하지 않으려고 둔 캐시다.
    //
    // ★ 이 3개에는 setter를 두지 않는다.
    //   엔티티로 읽고-더하고-쓰면(post.setLikeCount(post.getLikeCount()+1))
    //   동시 요청 시 lost update가 난다. 두 트랜잭션이 같은 5를 읽고 둘 다 6을 쓰면
    //   실제로는 2번 눌렸는데 6이 되는 식.
    //   → Repository의 @Modifying 원자적 UPDATE(SET like_count = like_count + 1)로만 갱신한다.
    //   설계정의서 §12 검수 항목 "동시성 검증"이 이걸 확인하는 항목이다.

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    /** 10 이상이면 관리자 페이지 노출 (임계값은 application.yml 설정값으로 뺀다) */
    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    /**
     * 숨김/삭제를 처리한 관리자. FK → users.user_id
     * 구 ADMINS 테이블이 USERS로 통합되었으므로 참조 대상은 users다. (§5.1)
     */
    @Column(name = "moderated_by_admin_id")
    private Long moderatedByAdminId;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    /**
     * @CreationTimestamp / @UpdateTimestamp = Hibernate가 값을 채운다.
     * DB에도 DEFAULT CURRENT_TIMESTAMP / ON UPDATE가 걸려 있어 양쪽 다 가능하지만,
     * Java에서 채우면 save() 직후 엔티티에 값이 들어 있어 응답 DTO를 바로 만들 수 있다.
     * (DB에만 맡기면 select를 한 번 더 하거나 refresh해야 값이 보인다)
     *
     * ※ 알아둘 것: view_count를 @Modifying 네이티브 UPDATE로 올리면 Hibernate를 거치지
     *   않으므로 DB의 ON UPDATE CURRENT_TIMESTAMP가 발동해 updated_at이 같이 바뀐다.
     *   즉 "조회만 해도 수정일시가 갱신"된다. 정렬 기준은 created_at을 쓰므로 지금은 무해하지만,
     *   updated_at으로 정렬하게 되면 이 점을 기억할 것.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA 전용 기본 생성자.
     * protected로 두는 이유: JPA는 리플렉션으로 쓰므로 protected면 충분하고,
     * public으로 열어두면 다른 코드가 빈 껍데기 엔티티를 만들 수 있게 된다.
     */
    protected BoardPost() {
    }

    private BoardPost(Long userId, Long recipeId, String title, String content) {
        this.userId = Objects.requireNonNull(userId, "userId는 필수입니다.");
        this.recipeId = Objects.requireNonNull(recipeId, "recipeId는 필수입니다.");
        this.title = Objects.requireNonNull(title, "title은 필수입니다.");
        this.content = Objects.requireNonNull(content, "content는 필수입니다.");
        this.status = PostStatus.PUBLISHED;
    }

    /**
     * 레시피 등록 시 자동 생성되는 유일한 생성 경로.
     *
     * 정적 팩토리로 만든 이유: 생성자를 public으로 열면 파라미터 4개가 전부 Long/String이라
     * 순서를 바꿔 넣어도 컴파일이 통과한다. 이름이 붙은 팩토리 메서드 하나만 열어두면
     * "게시글은 레시피로부터만 생긴다"는 규칙이 코드에 드러난다.
     */
    public static BoardPost ofRecipe(Long userId, Long recipeId, String title, String content) {
        return new BoardPost(userId, recipeId, title, content);
    }

    // ─── 상태 변경 메서드 ──────────────────────────────────────────
    // setStatus(PostStatus)를 열지 않고 의미 있는 이름의 메서드만 두는 이유:
    // 상태를 바꿀 때 moderated_by_admin_id와 moderated_at을 같이 채워야 하는데,
    // setter로 열어두면 상태만 바꾸고 처리자 기록을 빠뜨리는 실수가 생긴다.

    /** 신고 인정 → 숨김. recipes.is_active=FALSE는 호출 측(치연)에서 함께 처리해야 한다 */
    public void hide(Long adminId) {
        this.status = PostStatus.HIDDEN;
        this.moderatedByAdminId = adminId;
        this.moderatedAt = LocalDateTime.now();
    }

    /** 신고 기각 후 복구 */
    public void publish(Long adminId) {
        this.status = PostStatus.PUBLISHED;
        this.moderatedByAdminId = adminId;
        this.moderatedAt = LocalDateTime.now();
    }

    /** 소프트 삭제. 행은 남긴다 */
    public void softDelete(Long adminId) {
        this.status = PostStatus.DELETED;
        this.moderatedByAdminId = adminId;
        this.moderatedAt = LocalDateTime.now();
    }

    /**
     * 작성자 본인이 삭제. moderated_by_admin_id/moderated_at은 관리자 처리
     * 기록용이라 본인 삭제에는 채우지 않는다(신고 처리와 구분하기 위함).
     */
    public void deleteBySelf() {
        this.status = PostStatus.DELETED;
    }

    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /** 일반 사용자에게 노출 가능한 상태인지 */
    public boolean isVisible() {
        return this.status == PostStatus.PUBLISHED;
    }

    // ─── getter (Lombok을 쓴다면 클래스에 @Getter 하나로 대체 가능) ───

    public Long getPostId() {
        return postId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public PostStatus getStatus() {
        return status;
    }

    public int getViewCount() {
        return viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getReportCount() {
        return reportCount;
    }

    public Long getModeratedByAdminId() {
        return moderatedByAdminId;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
