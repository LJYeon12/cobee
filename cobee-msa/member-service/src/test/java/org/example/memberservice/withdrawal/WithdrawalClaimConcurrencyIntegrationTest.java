package org.example.memberservice.withdrawal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class WithdrawalClaimConcurrencyIntegrationTest {
    @Test
    void twoMemberJvmsAcceptOneOperationForTheSameMember() throws Exception {
        try (var fixture = new WithdrawalRecoveryIntegrationTest.Fixture(true)) {
            var outcomes = race("accept");
            assertEquals(outcomes.getFirst(), outcomes.getLast());
            assertEquals(1, fixture.sql.queryForObject("SELECT count(*) FROM withdrawal_job", Integer.class));
            assertEquals("WITHDRAWING", fixture.sql.queryForObject(
                    "SELECT lifecycle_status FROM member WHERE id=7", String.class));
        }
    }

    @Test
    void twoMemberJvmsCannotOwnTheSameUnexpiredJob() throws Exception {
        try (var fixture = new WithdrawalRecoveryIntegrationTest.Fixture(true)) {
            var operation = fixture.store.accept(7).operationId();
            var outcomes = race("claim");
            assertEquals(1, outcomes.stream().filter("NONE"::equals).count());
            assertEquals(1, outcomes.stream().filter(operation.toString()::equals).count());
            assertNull(fixture.store.claim(UUID.randomUUID()));
        }
    }

    private static java.util.List<String> race(String mode) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("build"), "withdrawal-member-race-").toAbsolutePath();
        var children = new ArrayList<Process>();
        try {
            for (int n = 0; n < 2; n++) {
                Path arguments = directory.resolve(n + ".args");
                String cp = runtimeClasspath();
                Files.writeString(arguments, "-cp\n\"" + cp.replace('\\', '/') + "\"\n"
                        + WithdrawalClaimConcurrencyIntegrationTest.class.getName() + "\n" + mode + "\n\""
                        + directory.toString().replace('\\', '/') + "\"\n" + n + "\n");
                children.add(new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                        "@" + arguments).redirectErrorStream(true)
                        .redirectOutput(directory.resolve(n + ".log").toFile()).start());
            }
            await(directory.resolve("ready-0"));
            await(directory.resolve("ready-1"));
            Files.writeString(directory.resolve("go"), "go");
            for (var child : children) {
                assertTrue(child.waitFor(45, TimeUnit.SECONDS));
                assertEquals(0, child.exitValue());
            }
            return java.util.List.of(Files.readString(directory.resolve("result-0")),
                    Files.readString(directory.resolve("result-1")));
        } finally {
            for (var child : children)
                if (child.isAlive())
                    child.destroyForcibly().waitFor();
        }
    }

    private static void await(Path marker) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        while (!Files.exists(marker)) {
            if (System.nanoTime() > deadline)
                throw new IllegalStateException("Fixture barrier timeout: " + marker.getFileName());
            Thread.sleep(10);
        }
    }

    private static String runtimeClasspath() throws java.io.IOException {
        String direct = System.getProperty("withdrawal.test.classpath");
        if (direct != null)
            return direct;
        String file = System.getProperty("withdrawal.test.classpath.file");
        return file == null ? System.getProperty("java.class.path") : Files.readString(Path.of(file));
    }

    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[1]);
        String participant = args[2];
        try (var fixture = new WithdrawalRecoveryIntegrationTest.Fixture(false)) {
            Files.writeString(directory.resolve("ready-" + participant), "ready");
            await(directory.resolve("go"));
            String result;
            if ("accept".equals(args[0])) {
                result = fixture.store.accept(7).operationId().toString();
            } else {
                var claim = fixture.store.claim(UUID.randomUUID());
                result = claim == null ? "NONE" : claim.id().toString();
            }
            Files.writeString(directory.resolve("result-" + participant), result);
        }
    }
}
