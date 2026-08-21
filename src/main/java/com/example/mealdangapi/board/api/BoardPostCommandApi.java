package com.example.mealdangapi.board.api;

import com.example.mealdangapi.board.dto.response.ReportedPostResponse;
import com.example.mealdangapi.global.common.PageResponse;

/**
 * ★ 게시판 도메인이 외부(치연 도메인)에 공개하는 유일한 진입점.
 *
 * 왜 인터페이스로 빼는가:
 *   치연이 BoardPostService를 직접 주입받으면 내부 구조 변경 시 치연 코드가 깨진다.
 *   인터페이스만 열어두면 구현을 자유롭게 바꿀 수 있고,
 *   치연 입장에서도 "게시판 쪽에서 쓸 수 있는 건 이것뿐"이 명확해진다.
 *
 * 구현체: com.example.mealdangapi.board.service.BoardPostCommandService
 *
 * ─── 치연 연동 정보 ────────────────────────────────────────
 *   인터페이스 : com.example.mealdangapi.board.api.BoardPostCommandApi
 *   주입       : private final BoardPostCommandApi boardPostCommandApi;
 *
 * ─── 트랜잭션 규칙 (중요) ──────────────────────────────────
 * 모든 메서드는 REQUIRED(기본값)로 동작한다. 호출한 쪽 트랜잭션에 그대로 참여한다.
 * REQUIRES_NEW를 쓰면 트랜잭션이 분리돼 일부만 반영되는 상태가 생긴다.
 * 협의자료 §4 "전체 작업을 하나의 @Transactional 범위에서 처리" 위배.
 */
public interface BoardPostCommandApi {

    // ═══════════════════════════════════════════════════════════
    //  레시피 등록 → 게시글 자동 생성
    // ═══════════════════════════════════════════════════════════

    /**
     * [정식 시그니처] 협의자료 PDF §3 기준.
     *
     * 호출 전제:
     *   recipes INSERT가 끝나 recipeId가 채번된 뒤에 호출해야 한다.
     *   board_posts.recipe_id에 FK가 걸려 있어 채번 전 호출은 FK 위반으로 실패한다.
     *
     * 호출 조건:
     *   sourceType == USER_SUBMISSION 인 레시피만 호출한다.
     *   공공 API·관리자 등록 레시피는 author_user_id가 없어 게시글을 만들 수 없다.
     *
     * @param recipeId     방금 저장된 레시피 ID
     * @param authorUserId 레시피 등록 회원 ID (= 게시글 작성자)
     * @return 생성된 게시글 ID (post_id)
     */
    Long createBoardPost(Long recipeId, Long authorUserId);

    /**
     * [호환용 오버로드] recipes.author를 역조회해 정식 메서드로 위임한다.
     * 작성자가 없는 레시피(공공 API·관리자 등록)면 예외가 발생한다.
     */
    Long createBoardPost(Long recipeId);

    // ═══════════════════════════════════════════════════════════
    //  관리자 신고 처리
    // ═══════════════════════════════════════════════════════════

    /**
     * 신고 수락 — 게시글 숨김 처리.
     *
     * ★ 반환 타입이 void → Long으로 변경되었습니다. (2026-08-20)
     *   치연 요청사항 3번 "postId → recipeId 조회" 대응입니다.
     *   별도 조회 메서드를 두는 대신 이 메서드가 연결 레시피 ID를 반환합니다.
     *   호출을 두 번 하지 않아도 되고, 그 사이에 상태가 바뀔 여지도 없습니다.
     *
     * 이 메서드가 처리하는 것:
     *   ① POST_REPORTS.status → 'ACCEPTED' (해당 게시글의 PENDING 건 전체)
     *   ② BOARD_POSTS.status  → 'HIDDEN'
     *
     * 호출 측(치연)이 같은 트랜잭션에서 처리할 것:
     *   ③ RECIPES.is_active → FALSE  ← 반환된 recipeId 사용
     *   ④ ADMIN_ACTIONS INSERT
     *
     * ③이 누락되면 게시판에서만 사라지고 추천에는 계속 노출됩니다.
     *
     * ※ 반환값이 null일 수 있습니다.
     *   board_posts.recipe_id가 ON DELETE SET NULL이라 연결 레시피가 없는
     *   게시글이 이론상 존재할 수 있습니다. null 확인 후 ③을 수행해주세요.
     *
     * @param postId  숨길 게시글 ID
     * @param adminId 처리한 관리자 ID (moderated_by_admin_id에 기록)
     * @return 연결된 레시피 ID. 연결 레시피가 없으면 null
     */
    Long hidePost(Long postId, Long adminId);

    /**
     * [호환용 오버로드] adminId 없이 호출.
     *
     * ※ moderated_by_admin_id가 NULL로 남아 처리자 추적이 불가능합니다.
     *   가급적 adminId를 포함한 쪽을 사용해주세요.
     */
    Long hidePost(Long postId);

    /**
     * 신고 기각 — PENDING 신고를 DISMISSED로 일괄 전환.
     *
     * 치연 요청사항 1번 대응입니다.
     *
     * 이 메서드가 처리하는 것:
     *   · POST_REPORTS.status → 'DISMISSED' (해당 게시글의 PENDING 건 전체)
     *
     * 처리하지 않는 것 (의도적):
     *   · BOARD_POSTS.status는 그대로 둡니다. PUBLISHED면 PUBLISHED로 유지됩니다.
     *   · 연결 레시피의 is_active도 건드리지 않습니다.
     *
     * 호출 측(치연)이 처리할 것:
     *   · ADMIN_ACTIONS INSERT
     *
     * ※ board_posts.report_count는 감소시키지 않습니다.
     *   이 값은 "누적 신고 횟수"이며, 기각되었다고 신고받은 사실이 사라지지는 않습니다.
     *   다만 관리자 목록 조회는 PENDING 건수 기준이라 기각된 게시글은 목록에서 빠집니다.
     *   (아래 getReportedPosts 참고)
     *
     * @param postId  대상 게시글 ID
     * @param adminId 처리한 관리자 ID (handled_by_admin_id에 기록)
     * @return 기각 처리된 신고 건수. 처리할 PENDING 신고가 없으면 0
     */
    int dismissPendingReports(Long postId, Long adminId);

    /**
     * 관리자 신고 목록 조회.
     *
     * 치연 요청사항 2번 대응입니다.
     * 미처리(PENDING) 신고가 임계값 이상 쌓인 게시글을 반환합니다.
     *
     * ★ 집계 기준이 board_posts.report_count가 아니라 PENDING 신고 건수입니다.
     *   report_count는 기각 후에도 줄지 않아 이미 처리한 게시글이 목록에 계속 남습니다.
     *   "아직 검토가 필요한 게시글"을 보여주는 것이 목적이므로 PENDING 건수로 셉니다.
     *
     * @param minReportCount 최소 미처리 신고 건수. 확정 사항은 10건이지만
     *                       관리자 화면에서 조정할 수 있도록 파라미터로 받습니다.
     * @param page           페이지 번호 (0부터)
     * @param size           페이지당 개수
     */
    PageResponse<ReportedPostResponse> getReportedPosts(
            int minReportCount, int page, int size);
}
