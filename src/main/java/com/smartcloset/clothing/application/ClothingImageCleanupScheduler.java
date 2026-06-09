package com.smartcloset.clothing.application;

import com.smartcloset.clothing.infrastructure.file.ClothingImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * DB transaction 결과에 맞춰 되돌릴 수 없는 image file cleanup을 지연 실행한다.
 */
@Component
public class ClothingImageCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClothingImageCleanupScheduler.class);

    private final ClothingImageStorage clothingImageStorage;

    public ClothingImageCleanupScheduler(ClothingImageStorage clothingImageStorage) {
        this.clothingImageStorage = clothingImageStorage;
    }

    public void deleteAfterCommit(String storedFilename) {
        if (!hasText(storedFilename)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteSafely(storedFilename, "without active transaction");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteSafely(storedFilename, "after commit");
            }
        });
    }

    public void deleteAfterRollback(String storedFilename) {
        if (!hasText(storedFilename) || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteSafely(storedFilename, "after rollback");
                    return;
                }
                if (status == TransactionSynchronization.STATUS_UNKNOWN) {
                    log.warn("Skipping clothing image rollback cleanup because transaction status is unknown. storedFilename={}",
                            storedFilename);
                }
            }
        });
    }

    public void deleteNowAddingSuppressed(String storedFilename, RuntimeException originalException) {
        if (!hasText(storedFilename)) {
            return;
        }
        try {
            clothingImageStorage.delete(storedFilename);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
            log.warn("Failed to cleanup clothing image after DB failure. storedFilename={}", storedFilename,
                    cleanupException);
        }
    }

    private void deleteSafely(String storedFilename, String reason) {
        try {
            clothingImageStorage.delete(storedFilename);
        } catch (RuntimeException exception) {
            log.warn("Failed to cleanup clothing image {}. storedFilename={}", reason, storedFilename, exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
