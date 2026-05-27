package com.smartcloset.auth.repository;

import com.smartcloset.auth.domain.AccountActionToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, Long> {

    Optional<AccountActionToken> findByTokenHash(String tokenHash);
}
