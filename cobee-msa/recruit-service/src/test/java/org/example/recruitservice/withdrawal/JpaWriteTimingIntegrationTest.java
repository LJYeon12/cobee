package org.example.recruitservice.withdrawal;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.recruitservice.client.MemberClient;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.recruit.RecruitRequest;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Small, local cost observation. HTTP providers are stubs; this is not a
 * service load benchmark.
 */
@Tag("integration")
public class JpaWriteTimingIntegrationTest {
    @Test
    void observeSameAndDifferentMemberWriteCosts() throws Exception {
        var observations = new LinkedHashMap<String, Object>();
        try (var fixture = new JpaWithdrawalIntegrationTest.Fixture()) {
            var members = fixture.context.getBean(MemberClient.class);
            when(members.getMyInfo(anyLong())).thenAnswer(i -> ApiResponse.success("fixture", "200",
                    MemberCoreResponse.builder().id(i.<Long>getArgument(0)).name("fixture").build()));
            var request = new ObjectMapper().readValue("{\"title\":\"fixture\",\"address\":\"fixture\"}",
                    RecruitRequest.class);
            fixture.reset();
            for (int n = 0; n < 5; n++)
                fixture.service.createRecruitPost(request, 7L);
            for (boolean same : List.of(true, false)) {
                fixture.reset();
                for (long id = 100; id < 104; id++)
                    fixture.sql.update("INSERT INTO member_write_state(member_id,state) VALUES(?,'ACTIVE')", id);
                var times = new ConcurrentLinkedQueue<Double>();
                List<Future<?>> futures = new ArrayList<>();
                long start = System.nanoTime();
                try (var pool = Executors.newFixedThreadPool(4)) {
                    for (int n = 0; n < 32; n++) {
                        long memberId = same ? 7L : 100L + n % 4;
                        futures.add(pool.submit(() -> {
                            long began = System.nanoTime();
                            fixture.service.createRecruitPost(request, memberId);
                            times.add((System.nanoTime() - began) / 1_000_000.0);
                        }));
                    }
                    for (var future : futures)
                        future.get(15, TimeUnit.SECONDS);
                }
                var sorted = times.stream().sorted().toList();
                assertEquals(32, sorted.size());
                assertEquals(32, fixture.sql.queryForObject("SELECT count(*) FROM recruit_post", Integer.class));
                observations.put(same ? "same_member" : "different_members",
                        Map.of("requests", 32, "threads", 4, "p50_ms", sorted.get(15), "p95_ms", sorted.get(30),
                                "wall_ms", (System.nanoTime() - start) / 1_000_000.0));
            }
        }
        Path output = Path.of(System.getProperty("withdrawal.test.output", "build"));
        Files.createDirectories(output);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("timing.json").toFile(),
                observations);
        System.out.println("WITHDRAWAL_TIMING " + new ObjectMapper().writeValueAsString(observations));
    }
}
