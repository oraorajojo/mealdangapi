package com.example.mealdangapi.board.api;

/**
 * ★ 게시판 도메인이 외부(치연 코드)에 공개하는 유일한 진입점.
 *
 * 왜 인터페이스로 빼는가:
 *   치연이 BoardPostService를 직접 주입받으면, 내가 서비스 내부 구조를 바꿀 때마다
 *   치연 코드가 컴파일 에러로 깨진다. 인터페이스만 열어두면 구현을 자유롭게 바꿀 수 있고,
 *   치연 입장에서도 "게시판 쪽에서 쓸 수 있는 건 이것뿐"이 명확해진다.
 *
 * 구현체: com.example.mealdangapi.board.service.BoardPostCommandService
 *
 * ─── 치연에게 전달할 정보 (카톡 회신 요청사항) ────────────────────
 *   인터페이스 : com.example.mealdangapi.board.api.BoardPostCommandApi
 *   주입 방법  : private final BoardPostCommandApi boardPostCommandApi;
 *   반환 타입  : createBoardPost → Long (생성된 post_id) / hidePost → void
 *
 * ─── 트랜잭션 규칙 (중요) ──────────────────────────────────────
 * 모든 메서드는 REQUIRED(기본값)로 동작한다. 호출한 쪽 트랜잭션에 그대로 참여한다는 뜻.
 * REQUIRES_NEW를 쓰면 트랜잭션이 분리돼서, 레시피 저장은 롤백됐는데 게시글만 남는
 * 부분 반영 상태가 생긴다. 협의자료 §4 "전체 작업을 하나의 @Transactional 범위에서
 * 처리하여 일부 작업 실패 시 전체 롤백을 보장한다"에 위배된다.
 * → 구현체에 절대 REQUIRES_NEW를 붙이지 않는다.
 */
public interface BoardPostCommandApi {

    // ═══════════════════════════════════════════════════════════
    //  레시피 등록 → 게시글 자동 생성
    // ═══════════════════════════════════════════════════════════

    /**
     * [정식 시그니처] 협의자료 PDF §3 기준.
     *
     * board_posts.user_id가 NOT NULL이므로 작성자 ID가 반드시 필요하다.
     * 치연은 레시피 등록 시점에 이미 이 값을 가지고 있으므로 그대로 넘기는 게 가장 깔끔하다.
     *
     * 호출 전제:
     *   recipes INSERT가 끝나 recipeId가 채번된 뒤에 호출해야 한다.
     *   board_posts.recipe_id에 FK가 걸려 있어 채번 전 호출은 FK 위반으로 실패한다.
     *
     * title/content는 recipes에서 읽어 채운다(아래 오버로드 주석 참고).
     *
     * @param recipeId     방금 저장된 레시피 ID
     * @param authorUserId 레시피 등록 회원 ID (= 게시글 작성자)
     * @return 생성된 게시글 ID (post_id)
     */
    Long createBoardPost(Long recipeId, Long authorUserId);

    /**
     * [호환용 오버로드] 카톡 협의 시그니처.
     *
     * 치연이 createBoardPost(recipeId) 형태로 호출해도 컴파일·동작되도록 열어둔다.
     * 내부에서 recipes.user_id를 역조회해 위 정식 메서드로 위임한다.
     *
     * ※ 이 오버로드는 임시다. 이유:
     *   - 협의 원칙 §2 "타 담당 테이블을 직접 만지지 않는다"를 내가 어기게 된다
     *   - 치연이 이미 쥐고 있는 값을 DB 왕복해서 다시 꺼내는 낭비 SELECT가 발생한다
     *   → 코드 합칠 때 정식 시그니처로 옮기는 것을 제안할 것.
     *
     * @param recipeId 방금 저장된 레시피 ID
     * @return 생성된 게시글 ID (post_id)
     */
    Long createBoardPost(Long recipeId);

    // ═══════════════════════════════════════════════════════════
    //  신고 인정 → 게시글 숨김
    // ═══════════════════════════════════════════════════════════

    /**
     * [정식 시그니처] 신고 인정 시 게시글 숨김 처리.
     *
     * 협의자료 §4 — 아래 4가지가 하나의 트랜잭션으로 묶여야 한다.
     *   ① POST_REPORTS.status = 'ACCEPTED'   ← 이 메서드가 처리 (post_reports는 종선 테이블)
     *   ② BOARD_POSTS.status  = 'HIDDEN'     ← 이 메서드가 처리
     *   ③ RECIPES.is_active   = FALSE        ← 치연이 직접 처리
     *   ④ ADMIN_ACTIONS INSERT               ← 치연이 직접 처리
     *
     * ★ ①은 협의 문서에 빠져 있던 항목이다. post_reports가 종선 테이블이므로
     *   이 메서드가 함께 처리한다. 치연 쪽에서 중복 구현하지 않아도 된다.
     *
     * ★ ③을 빠뜨리면 게시판에서만 사라지고 추천에는 계속 노출된다. 반드시 함께 처리할 것.
     *
     * @param postId  숨길 게시글 ID
     * @param adminId 처리한 관리자 ID (board_posts.moderated_by_admin_id에 기록)
     */
    void hidePost(Long postId, Long adminId);

    /**
     * [호환용 오버로드] 카톡 협의 시그니처.
     *
     * ※ 주의: adminId를 받지 않으므로 moderated_by_admin_id가 NULL로 남는다.
     *   "누가 숨겼는지" 추적이 불가능해져 해당 컬럼이 무의미해진다.
     *   관리자 ID는 치연의 인증 컨텍스트에만 있어서 내가 알아낼 방법이 없다.
     *   → 코드 합칠 때 반드시 정식 시그니처로 전환할 것.
     *
     * @param postId 숨길 게시글 ID
     */
    void hidePost(Long postId);
}
