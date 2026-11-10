package org.example.memberservice.withdrawal;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import jakarta.persistence.EntityManagerFactory;
import org.example.memberservice.domain.*;
import org.example.memberservice.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
public class MemberJpaLifecycleIntegrationTest {
    @Configuration
    @EnableJpaAuditing
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = MemberRepository.class)
    static class JpaConfig {
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(javax.sql.DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setPackagesToScan("org.example.memberservice.domain");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(
                    Map.of("hibernate.hbm2ddl.auto", "create-drop", "hibernate.physical_naming_strategy",
                            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"));
            return factory;
        }
        @Bean
        PlatformTransactionManager transactionManager(EntityManagerFactory emf, javax.sql.DataSource ds) {
            var tx = new JpaTransactionManager(emf);
            tx.setDataSource(ds);
            return tx;
        }
    }
    static class Fixture implements AutoCloseable {
        final com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource();
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        final JdbcTemplate sql;
        final MemberRepository members;
        final WithdrawalStore store;
        Fixture() throws Exception {
            try (var c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:65438/postgres", "fixture", "");
                    var s = c.createStatement()) {
                s.execute("CREATE SCHEMA IF NOT EXISTS withdrawal_tdd_member_jpa");
            }
            ds.setJdbcUrl("jdbc:postgresql://127.0.0.1:65438/postgres?currentSchema=withdrawal_tdd_member_jpa");
            ds.setUsername("fixture");
            ds.setPassword("");
            ds.setMaximumPoolSize(6);
            ds.setMinimumIdle(0);
            sql = new JdbcTemplate(ds);
            context.registerBean(javax.sql.DataSource.class, () -> ds);
            context.registerBean(JdbcTemplate.class, () -> sql);
            context.register(JpaConfig.class, WithdrawalStore.class);
            context.refresh();
            Path migration = Path.of("db/withdrawal/001-member.sql");
            if (!Files.exists(migration))
                migration = Path.of("../db/withdrawal/001-member.sql");
            sql.execute(Files.readString(migration));
            sql.execute("TRUNCATE withdrawal_job");
            members = context.getBean(MemberRepository.class);
            store = context.getBean(WithdrawalStore.class);
        }
        public void close() {
            context.close();
            ds.close();
        }
    }
    @Test
    void sqlMigrationAllowsJpaToCreateAnActiveMember() throws Exception {
        try (var f = new Fixture()) {
            var member = f.members
                    .saveAndFlush(Member.builder().socialId("fixture-new").name("fixture").isCompleted(true).build());
            assertEquals("ACTIVE", f.sql.queryForObject("SELECT lifecycle_status FROM member WHERE id=?", String.class,
                    member.getId()));
        }
    }
    @Test
    void staleJpaMergeCannotUndoWithdrawalAndFinalDeleteCascades() throws Exception {
        try (var f = new Fixture()) {
            var member = f.members.saveAndFlush(
                    Member.builder().socialId("fixture-existing").name("fixture").isCompleted(true).build());
            f.context.getBean(PublicProfileRepository.class)
                    .saveAndFlush(new PublicProfile(null, null, false, false, false, "fixture", member));
            f.context.getBean(MemberPreferenceRepository.class)
                    .saveAndFlush(MemberPreference.builder().member(member).build());
            var stale = f.members.findById(member.getId()).orElseThrow();
            f.store.accept(member.getId());
            stale.update("updated", null);
            f.members.saveAndFlush(stale);
            assertEquals("WITHDRAWING", f.sql.queryForObject("SELECT lifecycle_status FROM member WHERE id=?",
                    String.class, member.getId()));
            for (String stage : List.of("RECRUIT_BLOCK", "RECRUIT_CLEANUP", "REFRESH_DELETE", "MEMBER_REMOVE"))
                f.store.advance(f.store.claim(UUID.randomUUID()), stage);
            f.store.complete(f.store.claim(UUID.randomUUID()));
            assertFalse(f.members.existsById(member.getId()));
            assertEquals(0, f.context.getBean(PublicProfileRepository.class).count());
            assertEquals(0, f.context.getBean(MemberPreferenceRepository.class).count());
            assertEquals("WITHDRAWN", f.store.status(member.getId()).state());
        }
    }
}
