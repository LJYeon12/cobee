package org.example.recruitservice.withdrawal;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
@Tag("integration")
public class MemberWriteGuardIntegrationTest {
    @Test
    void writerHoldingGuardCommitsBeforeCleanupInAll100Runs() throws Exception {
        try (var f = new WithdrawalRaceIntegrationTest.Fixture(true)) {
            var guard = f.context.getBean(MemberWriteGuard.class);
            var tx = new TransactionTemplate(f.context.getBean(PlatformTransactionManager.class));
            for (int n = 0; n < 100; n++) {
                f.reset();
                var locked = new CountDownLatch(1);
                var release = new CountDownLatch(1);
                var deletionStarted = new CountDownLatch(1);
                try (var pool = Executors.newFixedThreadPool(2)) {
                    var write = pool.submit(() -> tx.execute(status -> {
                        guard.requireWritable(7L);
                        locked.countDown();
                        try {
                            if (!release.await(10, TimeUnit.SECONDS))
                                throw new IllegalStateException("fixture timeout");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        f.sql.update("INSERT INTO recruit_post(owner_id,title) VALUES(7,'fixture')");
                        return null;
                    }));
                    assertTrue(locked.await(10, TimeUnit.SECONDS));
                    var deletion = pool.submit(() -> {
                        deletionStarted.countDown();
                        f.withdraw();
                        return null;
                    });
                    try {
                        assertTrue(deletionStarted.await(10, TimeUnit.SECONDS));
                        Thread.sleep(10);
                        assertFalse(deletion.isDone());
                    } finally {
                        release.countDown();
                    }
                    write.get(10, TimeUnit.SECONDS);
                    deletion.get(10, TimeUnit.SECONDS);
                    assertEquals(0, f.count());
                }
            }
            System.out.println("WITHDRAWAL_TDD writer_first iterations=100 orphan_rows=0");
        }
    }
    @Test
    void blockedRowCannotBeActivatedByDelayedRegistration() throws Exception {
        try (var f = new WithdrawalRaceIntegrationTest.Fixture(true)) {
            var lifecycle = f.context.getBean(RecruitMemberLifecycle.class);
            var id = UUID.randomUUID();
            lifecycle.block(7, id);
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> lifecycle.activate(7));
            lifecycle.cleanup(7, id);
            lifecycle.cleanup(7, id);
            assertEquals("BLOCKED",
                    f.sql.queryForObject("SELECT state FROM member_write_state WHERE member_id=7", String.class));
        }
    }
    @Test
    void guardCannotReleaseItsLocksBeforeCallerTransaction() throws Exception {
        try (var f = new WithdrawalRaceIntegrationTest.Fixture(true)) {
            assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                    () -> f.context.getBean(MemberWriteGuard.class).requireWritable(7L));
        }
    }
    @Test
    void blockingUninitializedMemberCreatesPermanentTombstone() throws Exception {
        try (var f = new WithdrawalRaceIntegrationTest.Fixture(true)) {
            f.sql.update("DELETE FROM member_write_state");
            var lifecycle = f.context.getBean(RecruitMemberLifecycle.class);
            lifecycle.block(7, UUID.randomUUID());
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> lifecycle.activate(7));
        }
    }
}
