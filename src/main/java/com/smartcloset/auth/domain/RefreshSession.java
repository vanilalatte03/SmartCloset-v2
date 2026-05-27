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

    public static RefreshSession issue(User user, String tokenHash, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        return new RefreshSession(user, tokenHash, issuedAt, expiresAt);
    }

    public void revoke(LocalDateTime revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
        }
    }

    public void replace(String replacedByTokenHash, LocalDateTime revokedAt) {
        revoke(revokedAt);
        this.replacedByTokenHash = requireTokenHash(replacedByTokenHash);
    }

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
