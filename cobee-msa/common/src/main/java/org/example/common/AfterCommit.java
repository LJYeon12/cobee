package org.example.common;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.*;
/**
 * Call after the transactional writer returns, so both locks and connection
 * resources are released.
 */
public final class AfterCommit {
    private AfterCommit() {
    }
    public static void run(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Notification requires a completed transaction");
        try {
            action.run();
        } catch (RuntimeException error) {
            LoggerFactory.getLogger(AfterCommit.class).warn("Post-commit notification failed: {}",
                    error.getClass().getSimpleName());
        }
    }
}
