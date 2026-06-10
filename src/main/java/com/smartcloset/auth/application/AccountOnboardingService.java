package com.smartcloset.auth.application;

import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 신규 계정 생성 이후 인증 핵심 transaction과 분리된 온보딩 작업을 조율한다.
 */
@Service
public class AccountOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(AccountOnboardingService.class);

    private final UserRepository userRepository;
    private final DefaultClothingPresetSeeder defaultClothingPresetSeeder;
    private final TransactionTemplate transactionTemplate;

    public AccountOnboardingService(
            UserRepository userRepository,
            DefaultClothingPresetSeeder defaultClothingPresetSeeder,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.defaultClothingPresetSeeder = defaultClothingPresetSeeder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 신규 계정 transaction이 commit된 뒤 별도 transaction에서 기본 옷 온보딩을 실행한다.
     */
    public void seedDefaultClothesForNewAccountAfterCommit(User user) {
        User requiredUser = Objects.requireNonNull(user, "user must not be null");
        Long userId = Objects.requireNonNull(requiredUser.getId(), "user id must not be null");
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            seedDefaultClothesForNewAccount(userId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                seedDefaultClothesForNewAccount(userId);
            }
        });
    }

    private void seedDefaultClothesForNewAccount(Long userId) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    userRepository.findById(userId).ifPresent(defaultClothingPresetSeeder::seedIfEmpty)
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to seed default clothes for new account. userId={}", userId, exception);
        }
    }
}
