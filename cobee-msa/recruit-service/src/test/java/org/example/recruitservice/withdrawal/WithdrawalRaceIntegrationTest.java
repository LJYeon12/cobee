package org.example.recruitservice.withdrawal;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.nhn.NhnStorageService;
import org.example.recruitservice.client.MemberClient;
import org.example.recruitservice.domain.RecruitPost;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.recruit.RecruitRequest;
import org.example.recruitservice.repository.*;
import org.example.recruitservice.service.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Actual application services, real PostgreSQL transactions, synthetic HTTP
 * boundaries.
 */
@Tag("integration")
public class WithdrawalRaceIntegrationTest {
    static final String URL = "jdbc:postgresql://127.0.0.1:65438/postgres?currentSchema=withdrawal_tdd_core";
    static final ObjectMapper JSON = new ObjectMapper();
    @Configuration
    @EnableTransactionManagement
    static class Transactions {
        @Bean
        PlatformTransactionManager transactionManager(javax.sql.DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }
    }
    static class Fixture implements AutoCloseable {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        final com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
        final JdbcTemplate sql = new JdbcTemplate(ds);
        final GoogleMapService maps = mock(GoogleMapService.class);
        final RecruitRepository posts = mock(RecruitRepository.class);
        final RecruitService service;
        Fixture(boolean reset) throws Exception {
            ds.setJdbcUrl(URL);
            ds.setUsername("fixture");
            ds.setPassword("");
            ds.setMaximumPoolSize(6);
            ds.setMinimumIdle(0);
            ds.setConnectionTimeout(5000);
            try (var c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:65438/postgres", "fixture", "");
                    var s = c.createStatement()) {
                s.execute("CREATE SCHEMA IF NOT EXISTS withdrawal_tdd_core");
            }
            sql.execute(
                    "CREATE TABLE IF NOT EXISTS member_write_state(member_id bigint PRIMARY KEY,state text NOT NULL,withdrawal_id uuid,cleaned boolean NOT NULL DEFAULT false)");
            sql.execute("CREATE TABLE IF NOT EXISTS recruit_post(id bigserial PRIMARY KEY,owner_id bigint,title text)");
            sql.execute("CREATE TABLE IF NOT EXISTS images(id bigserial PRIMARY KEY,recruit_post_id bigint)");
            sql.execute("CREATE TABLE IF NOT EXISTS recruit_image(id bigserial PRIMARY KEY,recruit_post_id bigint)");
            sql.execute(
                    "CREATE TABLE IF NOT EXISTS bookmark(id bigserial PRIMARY KEY,member_id bigint,recruit_post_id bigint)");
            sql.execute(
                    "CREATE TABLE IF NOT EXISTS apply_record(id bigserial PRIMARY KEY,applied_member_id bigint,post_id bigint)");
            sql.execute(
                    "CREATE TABLE IF NOT EXISTS comment(id bigserial PRIMARY KEY,comment_author_id bigint,post_id bigint,parent_id bigint)");
            if (reset)
                reset();
            when(posts.save(any())).thenAnswer(i -> {
                RecruitPost p = i.getArgument(0);
                Long id = sql.queryForObject("INSERT INTO recruit_post(owner_id,title) VALUES (?,?) RETURNING id",
                        Long.class, p.getOwnerId(), p.getTitle());
                ReflectionTestUtils.setField(p, "id", id);
                return p;
            });
            when(posts.findAllByOwnerId(anyLong())).thenAnswer(i -> sql.query(
                    "SELECT id,owner_id,title FROM recruit_post WHERE owner_id=?", (r, n) -> RecruitPost.builder()
                            .id(r.getLong(1)).ownerId(r.getLong(2)).title(r.getString(3)).build(),
                    (Long) i.getArgument(0)));
            doAnswer(i -> {
                for (RecruitPost p : (Iterable<RecruitPost>) i.getArgument(0))
                    sql.update("DELETE FROM recruit_post WHERE id=?", p.getId());
                return null;
            }).when(posts).deleteAll(any(Iterable.class));
            var members = mock(MemberClient.class);
            when(members.getMyInfo(anyLong())).thenReturn(ApiResponse.success("fixture", "200",
                    MemberCoreResponse.builder().id(7L).name("fixture").email("fixture@example.invalid").build()));
            when(maps.getGeocode(anyString())).thenReturn(geo());
            context.registerBean(javax.sql.DataSource.class, () -> ds);
            context.registerBean(JdbcTemplate.class, () -> sql);
            context.registerBean(RecruitRepository.class, () -> posts);
            context.registerBean(MemberClient.class, () -> members);
            context.registerBean(GoogleMapService.class, () -> maps);
            context.registerBean(NhnStorageService.class, () -> mock(NhnStorageService.class));
            context.registerBean(ImagesRepository.class, () -> mock(ImagesRepository.class));
            context.register(Transactions.class);
            // The identical test runs before and after. Only application classes available
            // on its classpath differ.
            for (String name : List.of("MemberWriteGuard", "RecruitPostWriter", "RecruitMemberLifecycle")) {
                try {
                    context.register(Class.forName("org.example.recruitservice.withdrawal." + name));
                } catch (ClassNotFoundException beforeImplementation) {
                }
            }
            context.register(RecruitService.class);
            context.refresh();
            service = context.getBean(RecruitService.class);
        }
        void reset() {
            sql.execute(
                    "TRUNCATE recruit_post,member_write_state,images,recruit_image,bookmark,apply_record,comment RESTART IDENTITY");
            sql.update("INSERT INTO member_write_state(member_id,state) VALUES(7,'ACTIVE')");
        }
        void withdraw() throws Exception {
            try {
                var type = Class.forName("org.example.recruitservice.withdrawal.RecruitMemberLifecycle");
                Object lifecycle = context.getBean(type);
                UUID operation = UUID.fromString("00000000-0000-0000-0000-000000000007");
                type.getMethod("block", long.class, UUID.class).invoke(lifecycle, 7L, operation);
                type.getMethod("cleanup", long.class, UUID.class).invoke(lifecycle, 7L, operation);
            } catch (ClassNotFoundException beforeImplementation) {
                service.deleteAllRecruitData(7L);
            }
        }
        int count() {
            return sql.queryForObject("SELECT count(*) FROM recruit_post WHERE owner_id=7", Integer.class);
        }
        public void close() {
            context.close();
            ds.close();
        }
    }
    static Map<String, Object> geo() {
        return Map.of("latitude", 37.0, "longitude", 127.0, "formattedAddress", "fixture");
    }
    static RecruitRequest request() throws Exception {
        return JSON.readValue("{\"title\":\"fixture\",\"address\":\"fixture\",\"recruitCount\":2}",
                RecruitRequest.class);
    }
    static int lateWrites(int iterations) throws Exception {
        int orphans = 0;
        try (var f = new Fixture(true)) {
            for (int n = 0; n < iterations; n++) {
                f.reset();
                var entered = new CountDownLatch(1);
                var resume = new CountDownLatch(1);
                when(f.maps.getGeocode(anyString())).thenAnswer(i -> {
                    entered.countDown();
                    if (!resume.await(10, TimeUnit.SECONDS))
                        throw new IllegalStateException("fixture timeout");
                    return geo();
                });
                try (var executor = Executors.newSingleThreadExecutor()) {
                    var write = executor.submit(() -> {
                        try {
                            f.service.createRecruitPost(request(), 7L);
                        } catch (RuntimeException rejected) {
                            if (f.sql.queryForObject("SELECT state FROM member_write_state WHERE member_id=7",
                                    String.class).equals("ACTIVE"))
                                throw rejected;
                        }
                        return null;
                    });
                    try {
                        assertTrue(entered.await(10, TimeUnit.SECONDS));
                        f.withdraw();
                    } finally {
                        resume.countDown();
                    }
                    write.get(10, TimeUnit.SECONDS);
                }
                orphans += f.count();
            }
        }
        return orphans;
    }
    @Test
    void completedWithdrawalRejectsAll100PreviouslyAdmittedWrites() throws Exception {
        int orphans = lateWrites(100);
        System.out.println("WITHDRAWAL_TDD late_write iterations=100 orphan_rows=" + orphans);
        assertEquals(0, orphans, "Already-admitted writes must not recreate withdrawn member data");
    }
    @Test
    void normalActiveMemberCanCreatePost() throws Exception {
        try (var f = new Fixture(true)) {
            f.service.createRecruitPost(request(), 7L);
            assertEquals(1, f.count());
        }
    }
    static void signal(Path directory, String name, int value) throws Exception {
        Files.writeString(directory.resolve(name), Integer.toString(value));
    }
    static void await(Path directory, String name, int value) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            Path file = directory.resolve(name);
            if (Files.exists(file) && Files.readString(file).equals(Integer.toString(value)))
                return;
            Thread.sleep(5);
        }
        throw new IllegalStateException("fixture signal timeout " + name + "=" + value);
    }
    @Test
    void separateJvmCannotRecreateDataAfter100Withdrawals() throws Exception {
        Path signals = Files.createTempDirectory(Path.of("build"), "withdrawal-jvm-");
        String classpath = runtimeClasspath();
        Path childArgs = signals.resolve("child.args").toAbsolutePath();
        Files.writeString(childArgs, "-cp\n\"" + classpath.replace('\\', '/') + "\"\n" + getClass().getName() + "\n\""
                + signals.toAbsolutePath().toString().replace('\\', '/') + "\"\n");
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                "@" + childArgs).redirectErrorStream(true).redirectOutput(signals.resolve("child.log").toFile())
                .start();
        int orphans = 0;
        try (var f = new Fixture(true)) {
            signal(signals, "go", 0);
            for (int n = 0; n < 100; n++) {
                await(signals, "ready", n);
                f.withdraw();
                signal(signals, "release", n);
                await(signals, "done", n);
                orphans += f.count();
                f.reset();
                signal(signals, "go", n + 1);
            }
            assertTrue(child.waitFor(30, TimeUnit.SECONDS));
            assertEquals(0, child.exitValue());
        } finally {
            if (child.isAlive())
                child.destroyForcibly().waitFor();
        }
        System.out.println("WITHDRAWAL_TDD two_jvm iterations=100 orphan_rows=" + orphans);
        assertEquals(0, orphans, "A separate JVM must not bypass withdrawal state");
    }
    public static void main(String[] args) throws Exception {
        Path signals = Path.of(args[0]);
        try (var f = new Fixture(false)) {
            for (int n = 0; n < 100; n++) {
                final int iteration = n;
                await(signals, "go", n);
                when(f.maps.getGeocode(anyString())).thenAnswer(i -> {
                    signal(signals, "ready", iteration);
                    await(signals, "release", iteration);
                    return geo();
                });
                try {
                    f.service.createRecruitPost(request(), 7L);
                } catch (RuntimeException rejected) {
                    if (!f.sql.queryForObject("SELECT state FROM member_write_state WHERE member_id=7", String.class)
                            .equals("BLOCKED"))
                        throw rejected;
                }
                signal(signals, "done", n);
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
