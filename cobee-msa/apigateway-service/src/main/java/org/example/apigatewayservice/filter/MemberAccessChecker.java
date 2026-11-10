package org.example.apigatewayservice.filter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
@Component
@RequiredArgsConstructor
public class MemberAccessChecker {
    private final ReactiveStringRedisTemplate redis;
    public Mono<Boolean> isActive(String memberId) {
        return redis.opsForValue().get("member:access:" + memberId).map("ACTIVE"::equals)
                .switchIfEmpty(Mono.error(new IllegalStateException("Member access state unavailable")));
    }
}
