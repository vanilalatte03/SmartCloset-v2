package com.smartcloset.auth.repository;

import com.smartcloset.auth.domain.OAuthProvider;
import com.smartcloset.auth.domain.SocialAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    boolean existsByUserIdAndProvider(Long userId, OAuthProvider provider);

    List<SocialAccount> findByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SocialAccount account where account.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
