package com.smartcloset.auth.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 이메일 인증과 비밀번호 재설정에 쓰는 single-use 계정 토큰 entity다.
 *
 * <p>사용자가 받은 원문 토큰은 저장하지 않고, tokenHash로만 조회한다.</p>
 */
@Entity
@Table(
        name = "account_action_tokens",
        indexes = {
                @Index(name = "idx_account_action_tokens_user_purpose_created",
                        columnList = "user_id, purpose, created_at"),
                @Index(name = "idx_account_action_tokens_purpose_expires", columnList = "purpose, expires_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_account_action_tokens_token_hash", columnNames = "token_hash")
)
public class AccountActionToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private AccountActionTokenPurpose purpose;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    protected AccountActionToken() {
    }

    private AccountActionToken(
            User user,
            AccountActionTokenPurpose purpose,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.purpose = Objects.requireNonNull(purpose, "purpose must not be null");
        this.tokenHash = requireTokenHash(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    /**
     * 원문 token 대신 hash와 만료 시각만 저장하는 계정 액션 token을 발급한다.
     */
    public static AccountActionToken issue(
            User user,
            AccountActionTokenPurpose purpose,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        return new AccountActionToken(user, purpose, tokenHash, expiresAt);
    }

    /**
     * 목적이 맞고, 사용된 적 없고, 만료되지 않은 token만 소비할 수 있다.
     */
    public boolean canConsume(AccountActionTokenPurpose expectedPurpose, LocalDateTime now) {
        return purpose == expectedPurpose && usedAt == null && expiresAt.isAfter(now);
    }

    /**
     * token을 single-use로 만들기 위해 최초 사용 시각만 기록한다.
     */
    public void markUsed(LocalDateTime usedAt) {
        if (this.usedAt == null) {
            this.usedAt = Objects.requireNonNull(usedAt, "usedAt must not be null");
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public AccountActionTokenPurpose getPurpose() {
        return purpose;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    private String requireTokenHash(String tokenHash) {
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        if (tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash must not be blank");
        }
        if (tokenHash.length() > 255) {
            throw new IllegalArgumentException("tokenHash must be 255 characters or less");
        }
        return tokenHash;
    }
}
