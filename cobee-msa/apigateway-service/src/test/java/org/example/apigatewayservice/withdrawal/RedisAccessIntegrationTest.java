package org.example.apigatewayservice.withdrawal;
import java.time.Duration;
import org.example.apigatewayservice.filter.MemberAccessChecker;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;
import static org.junit.jupiter.api.Assertions.*;
@Tag("integration")
public class RedisAccessIntegrationTest {
    @Test
    void realRedisStateAndMissingStateAreBothChecked() throws Exception {
        var factory = new LettuceConnectionFactory("127.0.0.1", 6389);
        factory.afterPropertiesSet();
        factory.start();
        try {
            var redis = new StringRedisTemplate(factory);
            var checker = new MemberAccessChecker(new ReactiveStringRedisTemplate(factory));
            String key = "member:access:99001";
            redis.opsForValue().set(key, "ACTIVE");
            assertEquals(Boolean.TRUE, checker.isActive("99001").block(Duration.ofSeconds(3)));
            redis.opsForValue().set(key, "BLOCKED");
            assertEquals(Boolean.FALSE, checker.isActive("99001").block(Duration.ofSeconds(3)));
            redis.delete(key);
            assertThrows(IllegalStateException.class, () -> checker.isActive("99001").block(Duration.ofSeconds(3)));
        } finally {
            factory.destroy();
        }
    }
}
