package com.smartcloset.auth.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 서버가 관리하는 refresh token session이다.
 *
 * <p>tokenHash만 저장하고 raw token은 저장하지 않는다. rotation 시 기존 session은 revoke되고
 * 새 token hash가 replacedByTokenHash로 남는다.</p>
 */
@Entity
@Table(
        name = "refresh_sessions",
        indexes = {
                @Index(name = "idx_refresh_sessions_user_expires", columnList = "user_id, expires_at"),
                @Index(name = "idx_refresh_sessions_user_revoked", columnList = "user_id, revoked_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_sessions_token_hash", columnNames = "token_hash")
)
public class RefreshSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "replaced_by_token_hash", length = 255)
    private String replacedByTokenHash;

    protected RefreshSession() {
    }

    private RefreshSession(User user, String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.tokenHash = requireTokenHash(tokenHash);
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
    }

    /**
     * raw refresh token 대신 hash와 유효 기간만 저장하는 session을 발급한다.
     */
    public static RefreshSession issue(User user, String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        return new RefreshSession(user, tokenHash, issuedAt, expiresAt);
    }

    /**
     * 이미 revoke된 session은 같은 상태를 유지해 logout을 멱등하게 만든다.
     */
    public void revoke(LocalDateTime revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
        }
    }

    /**
     * rotation 시 현재 session을 revoke하고 다음 token hash를 추적용 metadata로 남긴다.
     */
    public void replace(String replacedByTokenHash, LocalDateTime revokedAt) {
        revoke(revokedAt);
        this.replacedByTokenHash = requireTokenHash(replacedByTokenHash);
    }

    /**
     * session이 아직 만료되지 않았고 revoke되지 않았을 때만 refresh에 사용할 수 있다.
     */
    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getReplacedByTokenHash() {
        return replacedByTokenHash;
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
