package org.example.apigatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {

    private final SecretKey key;
    private final MemberAccessChecker access;

    public AuthorizationHeaderFilter(@Value("${jwt.secret}") String secretKey, MemberAccessChecker access) {
        super(Config.class);
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.access = access;
    }

    public static class Config {

    }

    @Override
    public GatewayFilter apply(AuthorizationHeaderFilter.Config config) {
        // 여기에 필터로직 작성
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Authorization 헤더가 있는지 확인
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }
            // 2. 헤더에서 토큰 추출
            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer ", "");

            // 토큰에서 memberId 추출 & 유효성 검사
            String memberId = getMemberId(jwt);
            if (memberId == null) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            // 요청 헤더에 memberId 추가
            ServerHttpRequest newRequest = request.mutate().header("X-USER-ID", memberId).build();
            // 다음 필터로 요청 넘김
            String path = request.getURI().getPath();
            boolean lifecycleRequest = ("/auth/withdrawal".equals(path)
                    && request.getMethod() == org.springframework.http.HttpMethod.GET)
                    || ("/auth/withdraw".equals(path)
                            && request.getMethod() == org.springframework.http.HttpMethod.POST);
            if (lifecycleRequest)
                return chain.filter(exchange.mutate().request(newRequest).build());
            return Mono.defer(() -> access.isActive(memberId)).map(active -> active ? 1 : 0).onErrorReturn(-1)
                    .flatMap(state -> {
                        if (state == -1)
                            return onError(exchange, "Member access state unavailable", HttpStatus.SERVICE_UNAVAILABLE);
                        if (state == 0)
                            return onError(exchange, "Member access blocked", HttpStatus.FORBIDDEN);
                        return chain.filter(exchange.mutate().request(newRequest).build());
                    });
        };
    }

    // JWT를 파싱하여 memberId(subject)를 추출하는 메서드
    private String getMemberId(String jwt) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("JWT Token is not valid", e);
            return null;
        }
    }

    // 에러 발생 시 클라이언트에게 오류 응답을 보내는 메서드
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(err);
        return response.setComplete();
    }
}
