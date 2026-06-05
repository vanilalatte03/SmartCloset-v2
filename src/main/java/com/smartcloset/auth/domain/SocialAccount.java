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
 * 외부 OAuth provider 계정과 SmartCloset 사용자를 연결하는 JPA entity다.
 *
 * <p>Provider별 외부 사용자 id는 unique constraint로 보호해 같은 외부 계정이 여러 사용자에게 연결되지 않게 한다.</p>
 */
@Entity
@Table(
        name = "social_accounts",
        indexes = {
                @Index(name = "idx_social_accounts_user_provider", columnList = "user_id, provider"),
                @Index(name = "idx_social_accounts_email", columnList = "email")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_accounts_provider_user",
                columnNames = {"provider", "provider_user_id"}
        )
)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    protected SocialAccount() {
    }

    private SocialAccount(
            User user,
            OAuthProvider provider,
            String providerUserId,
            String email,
            LocalDateTime linkedAt
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.providerUserId = requireProviderUserId(providerUserId);
        this.email = requireEmail(email);
        this.linkedAt = Objects.requireNonNull(linkedAt, "linkedAt must not be null");
    }

    /**
     * 검증된 외부 provider 계정을 SmartCloset 사용자와 연결한다.
     */
    public static SocialAccount link(
            User user,
            OAuthProvider provider,
            String providerUserId,
            String email,
            LocalDateTime linkedAt
    ) {
        return new SocialAccount(user, provider, providerUserId, email, linkedAt);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }

    private String requireProviderUserId(String providerUserId) {
        Objects.requireNonNull(providerUserId, "providerUserId must not be null");
        if (providerUserId.isBlank()) {
            throw new IllegalArgumentException("providerUserId must not be blank");
        }
        if (providerUserId.length() > 255) {
            throw new IllegalArgumentException("providerUserId must be 255 characters or less");
        }
        return providerUserId;
    }

    private String requireEmail(String email) {
        Objects.requireNonNull(email, "email must not be null");
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (email.length() > 255) {
            throw new IllegalArgumentException("email must be 255 characters or less");
        }
        return email;
    }
}
