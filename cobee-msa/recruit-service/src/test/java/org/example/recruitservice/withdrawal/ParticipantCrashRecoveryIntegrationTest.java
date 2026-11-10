package org.example.recruitservice.withdrawal;

import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.recruitservice.dto.recruit.RecruitRequest;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class ParticipantCrashRecoveryIntegrationTest {
    @Configuration
    @EnableTransactionManagement
    static class Transactions {
        @Bean
        PlatformTransactionManager transactionManager(javax.sql.DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }
    }

    private Process child(String action, UUID operation, Path directory) throws Exception {
        String cp = System.getProperty("withdrawal.test.classpath");
        if (cp == null)
            cp = Files.readString(Path.of(System.getProperty("withdrawal.test.classpath.file")));
        Path arguments = directory.resolve(action + ".args").toAbsolutePath();
        Files.writeString(arguments, "-cp\n\"" + cp.replace('\\', '/') + "\"\n" + getClass().getName() + "\n" + action
                + "\n" + operation + "\n");
        return new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                "@" + arguments).redirectErrorStream(true).redirectOutput(directory.resolve(action + ".log").toFile())
                .start();
    }

    @Test
    void committedBlockSurvivesHaltedParticipantAndNewProcessCompletesCleanup() throws Exception {
        Path directory = Files.createTempDirectory(Path.of("build"), "withdrawal-participant-crash-");
        UUID operation = UUID.randomUUID();
        try (var fixture = new JpaWithdrawalIntegrationTest.Fixture()) {
            fixture.reset();
            var request = new ObjectMapper().readValue("{\"title\":\"fixture\",\"address\":\"fixture\"}",
                    RecruitRequest.class);
            fixture.service.createRecruitPost(request, 7L);
            Process halted = child("block-halt", operation, directory);
            try {
                assertTrue(halted.waitFor(60, TimeUnit.SECONDS));
                assertEquals(23, halted.exitValue());
            } finally {
                if (halted.isAlive())
                    halted.destroyForcibly().waitFor();
            }
            assertEquals("BLOCKED",
                    fixture.sql.queryForObject("SELECT state FROM member_write_state WHERE member_id=7", String.class));
            assertEquals(1, fixture.sql.queryForObject("SELECT count(*) FROM recruit_post", Integer.class));
            Process replacement = child("cleanup", operation, directory);
            try {
                assertTrue(replacement.waitFor(60, TimeUnit.SECONDS));
                assertEquals(0, replacement.exitValue());
            } finally {
                if (replacement.isAlive())
                    replacement.destroyForcibly().waitFor();
            }
            assertEquals(0, fixture.sql.queryForObject("SELECT count(*) FROM recruit_post", Integer.class));
            assertEquals(403, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> fixture.service.createRecruitPost(request, 7L)).getStatusCode().value());
        }
    }

    public static void main(String[] args) throws Exception {
        try (var ds = new com.zaxxer.hikari.HikariDataSource();
                var context = new AnnotationConfigApplicationContext()) {
            ds.setJdbcUrl("jdbc:postgresql://127.0.0.1:65438/postgres?currentSchema=withdrawal_tdd_jpa");
            ds.setUsername("fixture");
            ds.setPassword("");
            ds.setMaximumPoolSize(3);
            ds.setMinimumIdle(0);
            var jdbc = new JdbcTemplate(ds);
            context.registerBean(javax.sql.DataSource.class, () -> ds);
            context.registerBean(JdbcTemplate.class, () -> jdbc);
            context.register(Transactions.class, RecruitMemberLifecycle.class);
            context.refresh();
            var lifecycle = context.getBean(RecruitMemberLifecycle.class);
            UUID operation = UUID.fromString(args[1]);
            if (args[0].equals("block-halt")) {
                lifecycle.block(7, operation);
                Runtime.getRuntime().halt(23);
            } else
                lifecycle.cleanup(7, operation);
        }
    }
}
