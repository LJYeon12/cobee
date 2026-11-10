package org.example.memberservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.memberservice.domain.Member;
import org.example.memberservice.dto.member.MemberInfoDto;
import org.example.memberservice.dto.token.TokenRefreshResponse;
import org.example.memberservice.jwt.JwtProvider;
import org.example.memberservice.repository.MemberRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;
    private final org.example.memberservice.withdrawal.MemberAccessRegistry access;
    /**
     * RefreshToken을 받아서 새로운 AccessToken과 RefreshToken을 발급 RTR(Refresh Token
     * Rotation) 전략을 사용하여 매번 새로운 RefreshToken을 발급
     */
    public TokenRefreshResponse refreshTokens(String refreshToken) {
        // 1. RefreshToken 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }

        // 2. RefreshToken에서 memberId추출
        String memberId = jwtProvider.getMemberIdFromToken(refreshToken);
        memberRepository.findById(Long.valueOf(memberId))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "MEMBER_NOT_FOUND"))
                .requireActive();
        // 3. Redis에 저장된 RefreshToken과 비교
        String storedRefreshToken = redisTemplate.opsForValue().get(memberId);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND");
        }

        // 4. 새로운 AccessToken과 RefreshToken 생성
        String newAccessToken = jwtProvider.createAccessToken(memberId);
        String newRefreshToken = jwtProvider.createRefreshToken(memberId); // memberId 전달

        // 5. Redis에 새로운 RefreshToken 저장 (기존 것은 자동으로 덮어씌워짐)
        access.storeRefresh(Long.parseLong(memberId), newRefreshToken);
        log.info("토큰 재발급 완료 - memberId: {}", memberId);

        // 6. 응답 생성
        return TokenRefreshResponse.of(newAccessToken, newRefreshToken, jwtProvider.refreshTokenValiditySeconds);
    }

    public MemberInfoDto getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("해당 멤버를 찾을 수 없습니다."));
        member.requireActive();
        return MemberInfoDto.from(member);
    }

    public MemberInfoDto logout(Long memberId) {
        // Redis에서 RefreshToken 삭제
        redisTemplate.delete(String.valueOf(memberId));

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));

        return MemberInfoDto.from(member);
    }

}
