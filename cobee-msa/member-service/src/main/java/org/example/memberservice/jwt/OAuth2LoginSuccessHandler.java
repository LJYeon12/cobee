package org.example.memberservice.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.apiPayload.error.code.BaseErrorCode;
import org.example.common.apiPayload.error.exception.CustomException;
import org.example.memberservice.domain.Member;
import org.example.memberservice.repository.MemberRepository;
import org.example.memberservice.security.OAuthAttributes;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;
    private final org.example.memberservice.withdrawal.MemberAccessInitializer initializer;
    private final org.example.memberservice.withdrawal.MemberAccessRegistry access;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 로그인 성공 - 핸들러 처리");
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            // OAuthAttributes로 변환
            OAuthAttributes attributes = OAuthAttributes.of("response", oAuth2User.getAttributes());

            // DB에서 회원 정보 조회
            Member member = memberRepository.findBySocialId(attributes.getSocialId())
                    .orElseThrow(() -> new CustomException(BaseErrorCode._INTERNAL_SERVER_ERROR));// 일단 현재 코드를 사용하고 추후
                                                                                                  // common 라이브러리 코드 수정
            String memberId = String.valueOf(member.getId());
            initializer.ensureReady(member);
            String accessToken = jwtProvider.createAccessToken(memberId);
            String refreshToken = jwtProvider.createRefreshToken(memberId); // RefreshToken 생성 <- 새로운 유저일 경우만 생성하는 것으로
                                                                            // 수정해야함

            access.storeRefresh(member.getId(), refreshToken);

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            HashMap<Object, Object> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);

            response.getWriter().println(objectMapper.writeValueAsString(tokens));
        } catch (Exception e) {
            log.error("로그인 성공 후 처리 중 에러 발생", e);
            throw e;
        }
    }
}
