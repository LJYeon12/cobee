package org.example.memberservice.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {
    public final Key key;
    public final long accessTokenValiditySeconds;
    public final long refreshTokenValiditySeconds;

    // application.yml에 설정된 secret key를 가져와서 HMAC-SHA 알고리즘에 맞는 Key 객체로 변환
    public JwtProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expire-time}") long accessTokenValidity,
            @Value("${jwt.refresh-token-expire-time}") long refreshTokenValidity
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValiditySeconds = accessTokenValidity;
        this.refreshTokenValiditySeconds = refreshTokenValidity;
    }

    // MemberID를 받아 AccessToken을 생성하는 메서드
    public String createAccessToken(String memberId) {
        long now = (new Date()).getTime();
        // token 만료 시간
        Date validity = new Date(now + this.accessTokenValiditySeconds);

        return Jwts.builder()
                .setSubject(memberId) // token의 주체로 MemberId를 설정
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(validity)
                .compact();
    }

    // RefreshToken 생성
    public String createRefreshToken(String memberId) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.refreshTokenValiditySeconds);

        return Jwts.builder()
                .setSubject(memberId)
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(validity)
                .compact();
    }

    // RefreshToken에서 memberId를 추출하는 헬퍼 메서드
    public String getMemberIdFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("토큰에서 사용자 정보를 추출할 수 없습니다.");
        }
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("RefreshToken이 만료되었습니다.");
        } catch (Exception e) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
