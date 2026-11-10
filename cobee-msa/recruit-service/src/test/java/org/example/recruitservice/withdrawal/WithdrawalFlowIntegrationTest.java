package org.example.recruitservice.withdrawal;

import java.net.URI;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.*;
import org.example.common.apiPayload.error.handler.GlobalExceptionHandler;
import org.example.common.interceptor.InternalApiFilter;
import org.example.memberservice.client.RecruitClient;
import org.example.memberservice.controller.AuthController;
import org.example.memberservice.domain.Member;
import org.example.memberservice.repository.MemberRepository;
import org.example.memberservice.service.AuthService;
import org.example.memberservice.withdrawal.*;
import org.example.recruitservice.dto.recruit.RecruitRequest;
import org.junit.jupiter.api.*;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
public class WithdrawalFlowIntegrationTest {
    @Configuration
    @EnableTransactionManagement
    static class MemberTransactions {
        @Bean
        PlatformTransactionManager transactionManager(javax.sql.DataSource ds) {
            return new DataSourceTransactionManager(ds);
        }
    }

    @Test
    void acceptedRequestCompletesAcrossFeignAndBothDatabasesBeforeLateWriteIsRejected() throws Exception {
        try (var recruit = new JpaWithdrawalIntegrationTest.Fixture();
                var memberData = new com.zaxxer.hikari.HikariDataSource();
                var memberContext = new AnnotationConfigApplicationContext()) {
            recruit.reset();
            try (var connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:65438/postgres", "fixture",
                    ""); var statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS withdrawal_tdd_flow_member");
            }
            memberData
                    .setJdbcUrl("jdbc:postgresql://127.0.0.1:65438/postgres?currentSchema=withdrawal_tdd_flow_member");
            memberData.setUsername("fixture");
            memberData.setPassword("");
            memberData.setMaximumPoolSize(6);
            memberData.setMinimumIdle(0);
            var memberSql = new JdbcTemplate(memberData);
            memberSql.execute(
                    "CREATE TABLE IF NOT EXISTS member(id bigint PRIMARY KEY,lifecycle_status varchar(24) NOT NULL DEFAULT 'ACTIVE')");
            Path migration = Path.of("db/withdrawal/001-member.sql");
            if (!Files.exists(migration))
                migration = Path.of("../db/withdrawal/001-member.sql");
            memberSql.execute(Files.readString(migration));
            memberSql.execute("TRUNCATE withdrawal_job,member");
            memberSql.update("INSERT INTO member(id) VALUES(7)");
            var members = mock(MemberRepository.class);
            when(members.findById(7L)).thenAnswer(i -> {
                if (memberSql.queryForObject("SELECT count(*) FROM member WHERE id=7", Integer.class) == 0)
                    return Optional.empty();
                var member = Member.builder().socialId("fixture").isCompleted(true).build();
                ReflectionTestUtils.setField(member, "id", 7L);
                return Optional.of(member);
            });
            doAnswer(i -> {
                memberSql.update("DELETE FROM member WHERE id=7");
                return null;
            }).when(members).delete(any(Member.class));
            memberContext.registerBean(javax.sql.DataSource.class, () -> memberData);
            memberContext.registerBean(JdbcTemplate.class, () -> memberSql);
            memberContext.registerBean(MemberRepository.class, () -> members);
            memberContext.register(MemberTransactions.class, WithdrawalStore.class);
            memberContext.refresh();
            var store = memberContext.getBean(WithdrawalStore.class);
            var receiver = MockMvcBuilders
                    .standaloneSetup(
                            new RecruitLifecycleController(recruit.context.getBean(RecruitMemberLifecycle.class)))
                    .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new InternalApiFilter("fixture-key"))
                    .build();
            Client transport = (request, options) -> {
                try {
                    var builder = request(org.springframework.http.HttpMethod.valueOf(request.httpMethod().name()),
                            URI.create(request.url()).getPath());
                    request.headers().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
                    var response = receiver.perform(builder).andReturn().getResponse();
                    return Response.builder().request(request).status(response.getStatus()).reason("fixture")
                            .headers(Map.of()).body(response.getContentAsByteArray()).build();
                } catch (Exception error) {
                    throw new java.io.IOException(error);
                }
            };
            var client = Feign.builder().contract(new SpringMvcContract()).client(transport).target(RecruitClient.class,
                    "http://fixture.invalid");
            var factory = new LettuceConnectionFactory("127.0.0.1", 6389);
            factory.afterPropertiesSet();
            factory.start();
            try {
                var redis = new StringRedisTemplate(factory);
                redis.delete(List.of("7", MemberAccessRegistry.key(7)));
                var access = new MemberAccessRegistry(redis);
                access.activate(7);
                access.storeRefresh(7, "synthetic-refresh");
                var api = MockMvcBuilders
                        .standaloneSetup(new AuthController(mock(AuthService.class), new WithdrawalCoordinator(store)))
                        .setControllerAdvice(new GlobalExceptionHandler()).build();
                var entered = new CountDownLatch(1);
                var resume = new CountDownLatch(1);
                when(recruit.maps.getGeocode(anyString())).thenAnswer(i -> {
                    entered.countDown();
                    if (!resume.await(15, TimeUnit.SECONDS))
                        throw new IllegalStateException("fixture timeout");
                    return Map.of("latitude", 37.0, "longitude", 127.0, "formattedAddress", "fixture");
                });
                var request = new ObjectMapper().readValue("{\"title\":\"fixture\",\"address\":\"fixture\"}",
                        RecruitRequest.class);
                try (var pool = Executors.newSingleThreadExecutor()) {
                    var writing = pool.submit(() -> recruit.service.createRecruitPost(request, 7L));
                    try {
                        assertTrue(entered.await(15, TimeUnit.SECONDS));
                        api.perform(post("/auth/withdraw").header("X-USER-ID", "7")).andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.data.state").value("WITHDRAWING"));
                        var worker = new WithdrawalWorker(store, access, client, "fixture-key");
                        for (int n = 0; n < 20 && !"WITHDRAWN".equals(store.status(7).state()); n++)
                            worker.runOnce();
                        api.perform(get("/auth/withdrawal").header("X-USER-ID", "7")).andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.state").value("WITHDRAWN"));
                        api.perform(get("/auth/withdrawal").header("X-USER-ID", "8")).andExpect(status().isNotFound());
                    } finally {
                        resume.countDown();
                    }
                    var error = assertThrows(ExecutionException.class, () -> writing.get(15, TimeUnit.SECONDS));
                    assertInstanceOf(org.springframework.web.server.ResponseStatusException.class, error.getCause());
                    assertEquals(403, ((org.springframework.web.server.ResponseStatusException) error.getCause())
                            .getStatusCode().value());
                }
                assertEquals(0, recruit.sql.queryForObject("SELECT count(*) FROM recruit_post WHERE owner_id=7",
                        Integer.class));
                assertEquals(0, memberSql.queryForObject("SELECT count(*) FROM member", Integer.class));
                assertEquals("BLOCKED", redis.opsForValue().get(MemberAccessRegistry.key(7)));
                assertFalse(Boolean.TRUE.equals(redis.hasKey("7")));
            } finally {
                factory.destroy();
            }
        }
    }
}
