package org.example.memberservice.withdrawal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.example.memberservice.client.RecruitClient;
import org.example.memberservice.domain.Member;
import org.example.memberservice.repository.MemberRepository;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@Tag("integration")
public class WithdrawalRecoveryIntegrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void refreshAdmittedBeforeWithdrawalCannotRestoreCredentialAfterCompletion() throws Exception {
        try (var f = new Fixture(true)) {
            String secret = java.util.Base64.getEncoder().encodeToString(
                    "withdrawal-refresh-race-test-key-32bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var jwt = new org.example.memberservice.jwt.JwtProvider(secret, 86400000L, 604800000L);
            f.access.activate(7);
            String refresh = jwt.createRefreshToken("7");
            f.redis.opsForValue().set("7", refresh);
            var entered = new java.util.concurrent.CountDownLatch(1);
            var resume = new java.util.concurrent.CountDownLatch(1);
            Runnable pause = () -> {
                entered.countDown();
                try {
                    if (!resume.await(10, java.util.concurrent.TimeUnit.SECONDS))
                        throw new IllegalStateException("fixture timeout");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            };
            var redis = (org.springframework.data.redis.core.RedisTemplate<String, String>) mock(
                    org.springframework.data.redis.core.RedisTemplate.class);
            var values = (org.springframework.data.redis.core.ValueOperations<String, String>) mock(
                    org.springframework.data.redis.core.ValueOperations.class);
            when(redis.opsForValue()).thenReturn(values);
            when(values.get("7")).thenAnswer(i -> f.redis.opsForValue().get("7"));
            doAnswer(i -> {
                pause.run();
                f.redis.opsForValue().set(i.<String>getArgument(0), i.<String>getArgument(1), i.<Long>getArgument(2),
                        i.getArgument(3));
                return null;
            }).when(values).set(anyString(), anyString(), anyLong(), any(java.util.concurrent.TimeUnit.class));
            var registry = mock(MemberAccessRegistry.class, i -> {
                if (i.getMethod().getName().equals("storeRefresh"))
                    pause.run();
                try {
                    return i.getMethod().invoke(f.access, i.getArguments());
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            });
            var dependencies = java.util.Map.<Class<?>, Object>of(org.example.memberservice.jwt.JwtProvider.class, jwt,
                    org.springframework.data.redis.core.RedisTemplate.class, redis, MemberRepository.class, f.members,
                    MemberAccessRegistry.class, registry);
            var constructor = org.example.memberservice.service.AuthService.class.getConstructors()[0];
            var service = (org.example.memberservice.service.AuthService) constructor.newInstance(
                    java.util.Arrays.stream(constructor.getParameterTypes()).map(dependencies::get).toArray());
            try (var pool = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                var request = pool.submit(() -> service.refreshTokens(refresh));
                try {
                    assertTrue(entered.await(10, java.util.concurrent.TimeUnit.SECONDS));
                    f.store.accept(7);
                    for (int n = 0; n < 5; n++)
                        f.worker().runOnce();
                    assertEquals("WITHDRAWN", f.store.status(7).state());
                } finally {
                    resume.countDown();
                }
                try {
                    request.get(10, java.util.concurrent.TimeUnit.SECONDS);
                } catch (java.util.concurrent.ExecutionException e) {
                    assertInstanceOf(org.springframework.web.server.ResponseStatusException.class, e.getCause());
                    assertEquals(403, ((org.springframework.web.server.ResponseStatusException) e.getCause())
                            .getStatusCode().value());
                }
            }
            assertFalse(Boolean.TRUE.equals(f.redis.hasKey("7")),
                    "An already-admitted refresh must not recreate the credential after withdrawal");
        }
    }
    @Configuration
    @EnableTransactionManagement
    static class Transactions {
        @Bean
        PlatformTransactionManager transactionManager(javax.sql.DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }
    }
    static class Fixture implements AutoCloseable {
        final com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
        final JdbcTemplate sql;
        final MemberRepository members = mock(MemberRepository.class);
        final RecruitClient recruits = mock(RecruitClient.class);
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        final LettuceConnectionFactory connection = new LettuceConnectionFactory("127.0.0.1", 6389);
        final StringRedisTemplate redis;
        final MemberAccessRegistry access;
        final WithdrawalStore store;
        Fixture(boolean reset) throws Exception {
            try (var c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:65438/postgres", "fixture", "");
                    var s = c.createStatement()) {
                s.execute("CREATE SCHEMA IF NOT EXISTS withdrawal_tdd_member");
            }
            ds.setJdbcUrl("jdbc:postgresql://127.0.0.1:65438/postgres?currentSchema=withdrawal_tdd_member");
            ds.setUsername("fixture");
            ds.setPassword("");
            ds.setMaximumPoolSize(6);
            ds.setMinimumIdle(0);
            sql = new JdbcTemplate(ds);
            sql.execute(
                    "CREATE TABLE IF NOT EXISTS member(id bigint PRIMARY KEY,lifecycle_status varchar(24) NOT NULL DEFAULT 'ACTIVE')");
            Path migration = Path.of("db/withdrawal/001-member.sql");
            if (!Files.exists(migration))
                migration = Path.of("../db/withdrawal/001-member.sql");
            sql.execute(Files.readString(migration));
            connection.afterPropertiesSet();
            connection.start();
            redis = new StringRedisTemplate(connection);
            redis.afterPropertiesSet();
            access = new MemberAccessRegistry(redis);
            if (reset) {
                sql.execute("TRUNCATE withdrawal_job,member");
                sql.update("INSERT INTO member(id) VALUES(7)");
                redis.delete(List.of(MemberAccessRegistry.key(7), "7"));
            }
            when(members.findById(anyLong())).thenAnswer(i -> {
                Long id = i.getArgument(0);
                if (sql.queryForObject("SELECT count(*) FROM member WHERE id=?", Integer.class, id) == 0)
                    return Optional.empty();
                var m = Member.builder().name("fixture").socialId("fixture").isCompleted(true).build();
                ReflectionTestUtils.setField(m, "id", id);
                return Optional.of(m);
            });
            doAnswer(i -> {
                sql.update("DELETE FROM member WHERE id=?", ((Member) i.getArgument(0)).getId());
                return null;
            }).when(members).delete(any(Member.class));
            context.registerBean(javax.sql.DataSource.class, () -> ds);
            context.registerBean(JdbcTemplate.class, () -> sql);
            context.registerBean(MemberRepository.class, () -> members);
            context.register(Transactions.class, WithdrawalStore.class);
            context.refresh();
            store = context.getBean(WithdrawalStore.class);
        }
        WithdrawalWorker worker() {
            return new WithdrawalWorker(store, access, recruits, "synthetic-internal-key");
        }
        public void close() {
            context.close();
            connection.destroy();
            ds.close();
        }
    }
    @Test
    void repeatedAcceptanceReturnsOneDurableJob() throws Exception {
        try (var f = new Fixture(true)) {
            var first = f.store.accept(7);
            var second = f.store.accept(7);
            assertEquals(first.operationId(), second.operationId());
            assertEquals(1, f.sql.queryForObject("SELECT count(*) FROM withdrawal_job", Integer.class));
            assertEquals("WITHDRAWING",
                    f.sql.queryForObject("SELECT lifecycle_status FROM member WHERE id=7", String.class));
        }
    }
    @Test
    void expiredOwnerCannotAdvanceJobTakenByAnotherWorker() throws Exception {
        try (var f = new Fixture(true)) {
            f.store.accept(7);
            var old = f.store.claim(UUID.randomUUID());
            assertNull(f.store.claim(UUID.randomUUID()));
            f.sql.update("UPDATE withdrawal_job SET lease_until=CURRENT_TIMESTAMP-interval '1 second'");
            var replacement = f.store.claim(UUID.randomUUID());
            assertEquals(old.id(), replacement.id());
            assertThrows(IllegalStateException.class, () -> f.store.advance(old, "RECRUIT_BLOCK"));
            assertEquals("CACHE_BLOCK", f.store.status(7).stage());
        }
    }
    @Test
    void lostCleanupReplyCanResumeAfterApplicationRestart() throws Exception {
        UUID operation;
        try (var f = new Fixture(true)) {
            operation = f.store.accept(7).operationId();
            f.redis.opsForValue().set("7", "synthetic-refresh");
            var worker = f.worker();
            assertTrue(worker.runOnce());
            assertTrue(worker.runOnce());
            doThrow(new IllegalStateException("synthetic response lost after remote commit")).when(f.recruits)
                    .cleanup(7, operation, "synthetic-internal-key");
            worker.runOnce();
            assertEquals("RECRUIT_CLEANUP", f.store.status(7).stage());
            assertEquals("BLOCKED", f.redis.opsForValue().get(MemberAccessRegistry.key(7)));
        }
        try (var restarted = new Fixture(false)) {
            restarted.sql.update("UPDATE withdrawal_job SET next_attempt_at=CURRENT_TIMESTAMP");
            var worker = restarted.worker();
            assertTrue(worker.runOnce());
            assertTrue(worker.runOnce());
            assertTrue(worker.runOnce());
            assertEquals("WITHDRAWN", restarted.store.status(7).state());
            assertNull(restarted.redis.opsForValue().get("7"));
            assertEquals(0, restarted.sql.queryForObject("SELECT count(*) FROM member", Integer.class));
            assertEquals(operation, restarted.store.accept(7).operationId());
        }
    }
    @Test
    void finalMemberDeletionAndJobCompletionRollbackTogether() throws Exception {
        try (var f = new Fixture(true)) {
            f.store.accept(7);
            f.sql.update("UPDATE withdrawal_job SET stage='MEMBER_REMOVE'");
            var claim = f.store.claim(UUID.randomUUID());
            doThrow(new IllegalStateException("synthetic final commit failure")).when(f.members).flush();
            assertThrows(IllegalStateException.class, () -> f.store.complete(claim));
            assertEquals(1, f.sql.queryForObject("SELECT count(*) FROM member WHERE id=7", Integer.class));
            assertEquals("MEMBER_REMOVE", f.store.status(7).stage());
        }
    }
    @Test
    void delayedActivationCannotRemoveRedisTombstone() throws Exception {
        try (var f = new Fixture(true)) {
            f.access.block(7);
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> f.access.activate(7));
            assertEquals("BLOCKED", f.redis.opsForValue().get(MemberAccessRegistry.key(7)));
        }
    }
    @Test
    void cleanupCannotBeSkippedByCompletingAnEarlierStage() throws Exception {
        try (var f = new Fixture(true)) {
            f.store.accept(7);
            var claim = f.store.claim(UUID.randomUUID());
            assertThrows(IllegalStateException.class, () -> f.store.complete(claim));
            assertEquals(1, f.sql.queryForObject("SELECT count(*) FROM member WHERE id=7", Integer.class));
        }
    }
    @Test
    void stageCannotAdvanceOutOfOrder() throws Exception {
        try (var f = new Fixture(true)) {
            f.store.accept(7);
            var claim = f.store.claim(UUID.randomUUID());
            assertThrows(IllegalArgumentException.class, () -> f.store.advance(claim, "REFRESH_DELETE"));
        }
    }
    static Process child(String mode, Path marker) throws Exception {
        Path args = marker.resolveSibling(mode + ".args");
        String cp = runtimeClasspath();
        Files.writeString(args,
                "-cp\n\"" + cp.replace('\\', '/') + "\"\n" + WithdrawalRecoveryIntegrationTest.class.getName() + "\n"
                        + mode + "\n\"" + marker.toAbsolutePath().toString().replace('\\', '/') + "\"\n");
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                "@" + args.toAbsolutePath()).redirectErrorStream(true)
                .redirectOutput(marker.resolveSibling(mode + ".log").toFile()).start();
    }
    @Test
    void newJvmRecoversJobAfterClaimingJvmIsHalted() throws Exception {
        Path directory = Files.createTempDirectory(Path.of("build"), "withdrawal-recovery-");
        Path marker = directory.resolve("claimed");
        try (var f = new Fixture(true)) {
            var operation = f.store.accept(7).operationId();
            Process stopped = child("claim-halt", marker);
            try {
                assertTrue(stopped.waitFor(45, java.util.concurrent.TimeUnit.SECONDS));
                assertEquals(19, stopped.exitValue());
                assertEquals(operation.toString(), Files.readString(marker));
            } finally {
                if (stopped.isAlive())
                    stopped.destroyForcibly().waitFor();
            }
            f.sql.update("UPDATE withdrawal_job SET lease_until=CURRENT_TIMESTAMP-interval '1 second'");
            Process replacement = child("recover", directory.resolve("recovered"));
            try {
                assertTrue(replacement.waitFor(45, java.util.concurrent.TimeUnit.SECONDS));
                assertEquals(0, replacement.exitValue());
            } finally {
                if (replacement.isAlive())
                    replacement.destroyForcibly().waitFor();
            }
            assertEquals("WITHDRAWN", f.store.status(7).state());
        }
    }
    public static void main(String[] args) throws Exception {
        try (var f = new Fixture(false)) {
            if (args[0].equals("claim-halt")) {
                var claim = f.store.claim(UUID.randomUUID());
                Files.writeString(Path.of(args[1]), claim.id().toString());
                Runtime.getRuntime().halt(19);
            } else {
                for (int i = 0; i < 6; i++)
                    f.worker().runOnce();
                Files.writeString(Path.of(args[1]), f.store.status(7).state());
                if (!f.store.status(7).state().equals("WITHDRAWN"))
                    System.exit(2);
            }
        }
    }

    private static String runtimeClasspath() throws java.io.IOException {
        String value = System.getProperty("withdrawal.test.classpath");
        if (value != null)
            return value;
        String file = System.getProperty("withdrawal.test.classpath.file");
        return file == null ? System.getProperty("java.class.path") : Files.readString(Path.of(file));
    }
}
