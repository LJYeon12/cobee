package org.example.memberservice.withdrawal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Component
@RequiredArgsConstructor
public class MemberAccessRegistry {
    private static final DefaultRedisScript<Long> STORE_REFRESH = new DefaultRedisScript<>(
            "if redis.call('GET',KEYS[1]) ~= 'ACTIVE' then return 0 end redis.call('SET',KEYS[2],ARGV[1],'EX',ARGV[2]) return 1",
            Long.class);
    private static final DefaultRedisScript<Long> BLOCK = new DefaultRedisScript<>(
            "redis.call('SET',KEYS[1],'BLOCKED') redis.call('DEL',KEYS[2]) return 1", Long.class);
    private final RedisTemplate<String, String> redis;
    private static final DefaultRedisScript<Long> ACTIVATE = new DefaultRedisScript<>(
            "if redis.call('GET',KEYS[1]) == 'BLOCKED' then return 0 end redis.call('SET',KEYS[1],'ACTIVE') return 1",
            Long.class);
    public void activate(long memberId) {
        if (!Long.valueOf(1).equals(redis.execute(ACTIVATE, List.of(key(memberId)))))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MEMBER_ACCESS_BLOCKED");
    }
    public void block(long memberId) {
        if (!Long.valueOf(1).equals(redis.execute(BLOCK, List.of(key(memberId), String.valueOf(memberId)))))
            throw new IllegalStateException("Member block was not confirmed");
    }

    public void storeRefresh(long memberId, String token) {
        if (!Long.valueOf(1).equals(
                redis.execute(STORE_REFRESH, List.of(key(memberId), String.valueOf(memberId)), token, "604800")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "MEMBER_ACCESS_BLOCKED");
    }
    public void removeRefresh(long memberId) {
        redis.delete(String.valueOf(memberId));
    }
    public static String key(long memberId) {
        return "member:access:" + memberId;
    }
}
