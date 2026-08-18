package com.example.mealdangapi.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_users_email",
                        columnNames = "email"
                ),
                @UniqueConstraint(
                        name = "uq_users_nickname",
                        columnNames = "nickname"
                ),
                @UniqueConstraint(
                        name = "uq_users_social",
                        columnNames = {"social_provider", "social_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "social_provider",
            nullable = false,
            length = 20
    )
    private SocialProvider socialProvider = SocialProvider.NONE;

    @Column(name = "social_id", length = 255)
    private String socialId;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "cooking_level",
            nullable = false,
            length = 20
    )
    private CookingLevel cookingLevel = CookingLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 10
    )
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /**
     * 관리자 권한을 부여한 사용자
     * users.granted_by_user_id → users.user_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id")
    private User grantedBy;

    /**
     * 회원 탈퇴 사유
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "withdrawal_reason_code",
            length = 30
    )
    private WithdrawalReasonCode withdrawalReasonCode;

    /**
     * 기타 탈퇴 사유 및 세부 내용
     */
    @Column(
            name = "withdrawal_etc_reason",
            length = 500
    )
    private String withdrawalEtcReason;

    /**
     * 회원 탈퇴 일시
     */
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    public User(
            String email,
            String passwordHash,
            SocialProvider socialProvider,
            String socialId,
            String nickname,
            CookingLevel cookingLevel
    ) {
        this.email = email;
        this.passwordHash = passwordHash;

        this.socialProvider =
                socialProvider != null
                        ? socialProvider
                        : SocialProvider.NONE;

        this.socialId = socialId;
        this.nickname = nickname;

        this.cookingLevel =
                cookingLevel != null
                        ? cookingLevel
                        : CookingLevel.BEGINNER;

        this.role = UserRole.USER;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 회원 탈퇴 처리
     *
     * 실제 DB 행을 삭제하지 않고
     * 탈퇴 상태와 탈퇴 정보를 저장한다.
     */
    public void withdraw(
            WithdrawalReasonCode reasonCode,
            String etcReason
    ) {
        this.status = UserStatus.WITHDRAWN;
        this.withdrawalReasonCode = reasonCode;
        this.withdrawalEtcReason = etcReason;
        this.withdrawnAt = LocalDateTime.now();
    }
}