package com.example.mealdangapi.user.repository;

import com.example.mealdangapi.admin.dto.AdminUserListRow;
import com.example.mealdangapi.user.entity.SocialProvider;
import com.example.mealdangapi.user.entity.User;
import com.example.mealdangapi.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);

    Optional<User> findBySocialProviderAndSocialId(
            SocialProvider socialProvider,
            String socialId
    );

    /**
     * 관리자 "신고 유저 정지" 전체 목록 — ADMIN 역할은 제외하고 가입일 최신순.
     * 신고 횟수(board_posts.report_count 합산)는 이 유저가 쓴 게시글이 없으면
     * 서브쿼리가 0을 반환한다(COALESCE).
     *
     * countQuery를 직접 지정한 이유는 다른 곳과 동일 — 생성자 표현식이 들어간
     * 쿼리는 Spring Data가 count 쿼리를 자동으로 만들지 못한다.
     */
    @Query(
            value = "SELECT new com.example.mealdangapi.admin.dto.AdminUserListRow("
                    + "  u.userId, u.email, u.nickname, u.status, u.createdAt, "
                    + "  (SELECT COALESCE(SUM(p.reportCount), 0) FROM BoardPost p WHERE p.userId = u.userId)) "
                    + "FROM User u "
                    + "WHERE u.role <> :adminRole "
                    + "ORDER BY u.createdAt DESC",
            countQuery = "SELECT COUNT(u) FROM User u WHERE u.role <> :adminRole"
    )
    Page<AdminUserListRow> findAllNonAdminUsers(
            @Param("adminRole") UserRole adminRole,
            Pageable pageable
    );

    /** 위와 동일하되 신고 누적 횟수가 많은 순으로 정렬한다. */
    @Query(
            value = "SELECT new com.example.mealdangapi.admin.dto.AdminUserListRow("
                    + "  u.userId, u.email, u.nickname, u.status, u.createdAt, "
                    + "  (SELECT COALESCE(SUM(p.reportCount), 0) FROM BoardPost p WHERE p.userId = u.userId)) "
                    + "FROM User u "
                    + "WHERE u.role <> :adminRole "
                    + "ORDER BY (SELECT COALESCE(SUM(p2.reportCount), 0) FROM BoardPost p2 WHERE p2.userId = u.userId) DESC, "
                    + "  u.createdAt DESC",
            countQuery = "SELECT COUNT(u) FROM User u WHERE u.role <> :adminRole"
    )
    Page<AdminUserListRow> findAllNonAdminUsersByReportCountDesc(
            @Param("adminRole") UserRole adminRole,
            Pageable pageable
    );
}