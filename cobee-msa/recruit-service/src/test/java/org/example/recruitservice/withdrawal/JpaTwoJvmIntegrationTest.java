package org.example.recruitservice.withdrawal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.recruitservice.dto.recruit.RecruitRequest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@Tag("integration")
public class JpaTwoJvmIntegrationTest {
    static void signal(Path dir, String name, int n) throws Exception {
        Files.writeString(dir.resolve(name), Integer.toString(n));
    }
    static void await(Path dir, String name, int n) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        while (System.nanoTime() < until) {
            Path p = dir.resolve(name);
            if (Files.exists(p) && Files.readString(p).equals(Integer.toString(n)))
                return;
            Thread.sleep(5);
        }
        throw new IllegalStateException("fixture timeout: " + name);
    }
    @Test
    void actualJpaOnAnotherJvmCannotRecreate100WithdrawnPosts() throws Exception {
        int remaining = 0;
        Path dir = Files.createTempDirectory(Path.of("build"), "withdrawal-jpa-jvm-").toAbsolutePath();
        try (var f = new JpaWithdrawalIntegrationTest.Fixture()) {
            f.reset();
            Path arguments = dir.resolve("child.args");
            Files.writeString(arguments, "-Dwithdrawal.jpa.ddl=none\n-cp\n\"" + runtimeClasspath().replace('\\', '/')
                    + "\"\n" + getClass().getName() + "\n\"" + dir.toString().replace('\\', '/') + "\"\n");
            Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                    "@" + arguments).redirectErrorStream(true).redirectOutput(dir.resolve("child.log").toFile())
                    .start();
            try {
                long bootDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
                while (!Files.exists(dir.resolve("boot"))) {
                    if (!child.isAlive())
                        throw new IllegalStateException("Child JVM failed before readiness: " + child.exitValue());
                    if (System.nanoTime() > bootDeadline)
                        throw new IllegalStateException("Child JVM startup timeout");
                    Thread.sleep(10);
                }
                signal(dir, "go", 0);
                for (int n = 0; n < 100; n++) {
                    await(dir, "ready", n);
                    f.withdraw();
                    signal(dir, "release", n);
                    await(dir, "done", n);
                    remaining += f.sql.queryForObject("SELECT count(*) FROM recruit_post WHERE owner_id=7",
                            Integer.class);
                    f.reset();
                    signal(dir, "go", n + 1);
                }
                assertTrue(child.waitFor(45, TimeUnit.SECONDS));
                assertEquals(0, child.exitValue());
            } finally {
                if (child.isAlive())
                    child.destroyForcibly().waitFor();
            }
        }
        System.out.println("WITHDRAWAL_TDD actual_jpa_two_jvm iterations=100 orphan_rows=" + remaining);
        assertEquals(0, remaining);
    }
    public static void main(String[] args) throws Exception {
        Path dir = Path.of(args[0]);
        try (var f = new JpaWithdrawalIntegrationTest.Fixture()) {
            signal(dir, "boot", 0);
            for (int n = 0; n < 100; n++) {
                final int iteration = n;
                await(dir, "go", n);
                when(f.maps.getGeocode(anyString())).thenAnswer(i -> {
                    signal(dir, "ready", iteration);
                    await(dir, "release", iteration);
                    return Map.of("latitude", 37.0, "longitude", 127.0, "formattedAddress", "fixture");
                });
                var request = new ObjectMapper().readValue("{\"title\":\"fixture\",\"address\":\"fixture\"}",
                        RecruitRequest.class);
                try {
                    f.service.createRecruitPost(request, 7L);
                } catch (org.springframework.web.server.ResponseStatusException denied) {
                    if (denied.getStatusCode().value() != 403)
                        throw denied;
                }
                signal(dir, "done", n);
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
