package org.example.apigatewayservice.withdrawal;
import java.util.*;
import java.nio.charset.StandardCharsets;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.apigatewayservice.filter.AuthorizationHeaderFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
public class GatewayWithdrawalTest {
    static final String KEY = Base64.getEncoder()
            .encodeToString("withdrawal-gateway-test-key-32bytes".getBytes(StandardCharsets.UTF_8));
    static AuthorizationHeaderFilter filter(Mono<Boolean> allowed) throws Exception {
        try {
            var checkerType = Class.forName("org.example.apigatewayservice.filter.MemberAccessChecker");
            Object checker = mock(checkerType);
            when((Mono<Boolean>) checkerType.getMethod("isActive", String.class).invoke(checker, "7"))
                    .thenReturn(allowed);
            return AuthorizationHeaderFilter.class.getConstructor(String.class, checkerType).newInstance(KEY, checker);
        } catch (ClassNotFoundException beforeImplementation) {
            return AuthorizationHeaderFilter.class.getConstructor(String.class).newInstance(KEY);
        }
    }
    static int request(Mono<Boolean> allowed, String path) throws Exception {
        String token = Jwts.builder().setSubject("7").setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(KEY)), SignatureAlgorithm.HS256).compact();
        var builder = path.equals("/auth/withdrawal")
                ? MockServerHttpRequest.get(path)
                : MockServerHttpRequest.post(path);
        var exchange = MockServerWebExchange.from(builder.header("Authorization", "Bearer " + token).build());
        filter(allowed).apply(new AuthorizationHeaderFilter.Config()).filter(exchange, e -> {
            e.getResponse().setRawStatusCode(200);
            return Mono.empty();
        }).block();
        return exchange.getResponse().getStatusCode().value();
    }
    @Test
    void activeMemberCanWrite() throws Exception {
        assertEquals(200, request(Mono.just(true), "/recruit"));
    }
    @Test
    void withdrawnMemberCannotUseExistingAccessToken() throws Exception {
        assertEquals(403, request(Mono.just(false), "/recruit"));
    }
    @Test
    void unavailableRedisDoesNotAuthorizeWrite() throws Exception {
        assertEquals(503, request(Mono.error(new IllegalStateException("fixture Redis unavailable")), "/recruit"));
    }
    @Test
    void withdrawnMemberCanReadOwnWithdrawalStatus() throws Exception {
        assertEquals(200, request(Mono.just(false), "/auth/withdrawal"));
    }
    @Test
    void statusExceptionDoesNotMatchArbitraryAuthPaths() throws Exception {
        assertEquals(403, request(Mono.just(false), "/auth/withdrawal/other"));
    }
}
