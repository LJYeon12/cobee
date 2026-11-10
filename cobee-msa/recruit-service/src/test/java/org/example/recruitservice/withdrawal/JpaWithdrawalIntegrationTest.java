package org.example.recruitservice.withdrawal;
import java.util.*;
import java.sql.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.example.common.apiPayload.response.ApiResponse;
import org.example.common.nhn.NhnStorageService;
import org.example.recruitservice.client.MemberClient;
import org.example.recruitservice.dto.MemberCoreResponse;
import org.example.recruitservice.dto.recruit.RecruitRequest;
import org.example.recruitservice.repository.RecruitRepository;
import org.example.recruitservice.service.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@Tag("integration")
public class JpaWithdrawalIntegrationTest {
    @Configuration
    @EnableTransactionManagement
    @EnableJpaAuditing
    @EnableJpaRepositories(basePackageClasses = RecruitRepository.class)
    static class JpaConfiguration {
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(javax.sql.DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setPackagesToScan("org.example.recruitservice.domain");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto",
                    System.getProperty("withdrawal.jpa.ddl", "create-drop"), "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
            return factory;
        }
        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory em, javax.sql.DataSource ds) {
            var tx = new JpaTransactionManager(em);
            tx.setDataSource(ds);
            return tx;
        }
    }
    static class Fixture implements AutoCloseable {
        final com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        final GoogleMapService maps = mock(GoogleMapService.class);
        final JdbcTemplate sql;
        final RecruitService service;
        Fixture() throws Exception {
            try (var c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:65438/postgres", "fixture", "");
                    var s = c.createStatement()) {
                s.execute("CREATE SCHEMA IF NOT EXISTS withdrawal_tdd_jpa");
            }
            ds.setJdbcUrl("jdbc:postgresql://127.0.0.1:65438/postgres?currentSchema=withdrawal_tdd_jpa");
            ds.setUsername("fixture");
            ds.setPassword("");
            ds.setMaximumPoolSize(6);
            ds.setMinimumIdle(0);
            sql = new JdbcTemplate(ds);
            sql.execute(
                    "CREATE TABLE IF NOT EXISTS member_write_state(member_id bigint PRIMARY KEY,state text NOT NULL,withdrawal_id uuid,cleaned boolean NOT NULL DEFAULT false)");
            var members = mock(MemberClient.class);
            when(members.getMyInfo(7L)).thenReturn(
                    ApiResponse.success("fixture", "200", MemberCoreResponse.builder().id(7L).name("fixture").build()));
            when(maps.getGeocode(anyString()))
                    .thenReturn(Map.of("latitude", 37.0, "longitude", 127.0, "formattedAddress", "fixture"));
            context.registerBean(javax.sql.DataSource.class, () -> ds);
            context.registerBean(JdbcTemplate.class, () -> sql);
            context.registerBean(MemberClient.class, () -> members);
            context.registerBean(GoogleMapService.class, () -> maps);
            context.registerBean(NhnStorageService.class, () -> mock(NhnStorageService.class));
            context.register(JpaConfiguration.class, RecruitService.class);
            for (String name : List.of("MemberWriteGuard", "RecruitPostWriter", "RecruitMemberLifecycle"))
                try {
                    context.register(Class.forName("org.example.recruitservice.withdrawal." + name));
                } catch (ClassNotFoundException baseline) {
                }
            context.refresh();
            service = context.getBean(RecruitService.class);
        }
        void reset() {
            sql.execute(
                    "TRUNCATE recruit_post,images,bookmark,apply_record,comment,member_write_state RESTART IDENTITY CASCADE");
            sql.update("INSERT INTO member_write_state(member_id,state) VALUES(7,'ACTIVE')");
        }
        void withdraw() throws Exception {
            try {
                var t = Class.forName("org.example.recruitservice.withdrawal.RecruitMemberLifecycle");
                var bean = context.getBean(t);
                UUID id = UUID.fromString("00000000-0000-0000-0000-000000000007");
                t.getMethod("block", long.class, UUID.class).invoke(bean, 7L, id);
                t.getMethod("cleanup", long.class, UUID.class).invoke(bean, 7L, id);
            } catch (ClassNotFoundException baseline) {
                service.deleteAllRecruitData(7L);
            }
        }
        public void close() {
            context.close();
            ds.close();
        }
    }
    @Test
    void actualJpaCannotPersist100LatePostsAfterWithdrawal() throws Exception {
        int remaining = 0;
        try (var f = new Fixture()) {
            for (int n = 0; n < 100; n++) {
                f.reset();
                var entered = new CountDownLatch(1);
                var resume = new CountDownLatch(1);
                when(f.maps.getGeocode(anyString())).thenAnswer(i -> {
                    entered.countDown();
                    if (!resume.await(10, TimeUnit.SECONDS))
                        throw new IllegalStateException("fixture timeout");
                    return Map.of("latitude", 37.0, "longitude", 127.0, "formattedAddress", "fixture");
                });
                var request = new ObjectMapper().readValue("{\"title\":\"fixture\",\"address\":\"fixture\"}",
                        RecruitRequest.class);
                try (var pool = Executors.newSingleThreadExecutor()) {
                    var write = pool.submit(() -> {
                        try {
                            f.service.createRecruitPost(request, 7L);
                        } catch (org.springframework.web.server.ResponseStatusException denied) {
                            assertEquals(403, denied.getStatusCode().value());
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
                remaining += f.sql.queryForObject("SELECT count(*) FROM recruit_post WHERE owner_id=7", Integer.class);
            }
        }
        System.out.println("WITHDRAWAL_TDD actual_jpa iterations=100 orphan_rows=" + remaining);
        assertEquals(0, remaining);
    }
}
